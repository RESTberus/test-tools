# Morest

It's a fully automated testing framework for RESTful APIs.

### Key Features
- [x] Reinforcement Learning Enabled Instance Generation

### Installation

Firstly, you should install the required packages.

```bash
pip install -r requirements.txt
```

### Usage

```bash
usage: main.py [-h] [--yaml_path YAML_PATH] [--time_budget TIME_BUDGET] [--warm_up_times WARM_UP_TIMES] [--url URL]

optional arguments:
  -h, --help            show this help message and exit
  --yaml_path YAML_PATH
  --time_budget TIME_BUDGET
  --warm_up_times WARM_UP_TIMES
  --url URL
```

### TODO

- [ ] Add result output
- [ ] Add more sequence generation strategies (WIP)
- [ ] Add more instance generation strategies (WIP)
- [ ] Evaluation on large scale APIs

### Reference

If you use Morest in your research, please cite the following paper:

```bibtex
@inproceedings{liu2022morest,
  title={Morest: Model-based RESTful API testing with execution feedback},
  author={Liu, Yi and Li, Yuekang and Deng, Gelei and Liu, Yang and Wan, Ruiyuan and Wu, Runchao and Ji, Dandan and Xu, Shiheng and Bao, Minli},
  booktitle={Proceedings of the 44th International Conference on Software Engineering},
  pages={1406--1417},
  year={2022}
}
```
