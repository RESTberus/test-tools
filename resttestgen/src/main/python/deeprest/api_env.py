import numpy as np
import gymnasium as gym
from gymnasium import spaces
import os

PRINT_LOG = False
OBS_MAX = 20 # It's an uint8! Choose 0-255

# Real API from RestTestGen
class ApiEnv(gym.Env):

    actions_count = 0
    observation = []

    def __init__(self):
        super(ApiEnv, self).__init__()

        # Create named pipes, if not already done.
        try:
            os.mkfifo('j2p')
        except:
            pass
        try:
            os.mkfifo('p2j')
        except:
            pass

        # Open pipes persistently
        print("Waiting for Java to connect to pipes.")
        self.j2p_fifo = open('j2p', 'r')
        self.p2j_fifo = open('p2j', 'w')

        # Wait for number of available actions from Java
        print("Waiting for size of action space from Java.")
        line = self.j2p_fifo.readline()
        self.actions_count = int(line.strip())
        print(f"Received number of actions: {self.actions_count}")
        if (self.actions_count > 999):
            print("WARNING: more than 999 actions! Fix string encoding in step method.")

        # Init actions and observations spaces
        self.action_space = spaces.Discrete(self.actions_count)
        self.observation_space = spaces.Box(
            low=0,
            high=OBS_MAX,
            shape=(1, self.actions_count),
            dtype=np.uint8
        )

    def step(self, action):

        if PRINT_LOG:
            print(f"Next action is: {action}")

        # Send chosen action to Java
        self.p2j_fifo.write(f'{action:04}')
        self.p2j_fifo.flush()

        if PRINT_LOG:
            print("Action sent! Waiting for outcome from Java.")

        # Read outcome from Java
        line = self.j2p_fifo.readline()
        status_code = int(line.strip())

        if PRINT_LOG:
            print(f"Operation tested with status code: {status_code}")

        # Termination: the agent reaches the goal
        # Truncation: the episode is truncated as the agent consumed all the budget without accomplishing the goal
        observation, reward, terminated, truncated = self.compute_observation_and_reward(action, status_code)

        if PRINT_LOG:
            print(observation)

        info = {}
        return observation, reward, terminated, truncated, info

    def reset(self, seed=None, options=None):
        # Nothing can be done to reset the API...
        # Just reset the observation
        self.observation = np.zeros(
            shape=(1, self.actions_count),
            dtype=np.uint8
        )
        info = {}
        return self.observation, info

    def render(self):
        return
    
    def __del__(self):
        self.close()

    def close(self):
        if hasattr(self, 'j2p_fifo') and self.j2p_fifo:
            self.j2p_fifo.close()
        if hasattr(self, 'p2j_fifo') and self.p2j_fifo:
            self.p2j_fifo.close()
        return

    def compute_observation_and_reward(self, action, status_code):

        is_already_covered = self.observation[0][action] > 0
        is_successful = status_code >= 200 and status_code < 300
        truncated = False

        if is_successful:

            # Increase successes in observation (up to OBS_MAX)
            if self.observation[0][action] < OBS_MAX:
                self.observation[0][action] += 1

            # If an operation has been executed for more than OBS_MAX times, than trucate this episode
            else:
                truncated = True

            # Compute reward
            if is_already_covered:
                reward = -100
            else:
                reward = 1000

        else:

            # Compute reward
            if is_already_covered:
                reward = -100
            else:
                reward = -1

        terminated = True
        i = 0
        while i < self.actions_count:
            if self.observation[0][i] == 0:
                terminated = False
                break
            i += 1

        return self.observation, reward, terminated, truncated
