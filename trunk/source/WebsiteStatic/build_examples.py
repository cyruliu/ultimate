import collections
import glob
import json
import os
import shutil

import os.path as osp
""" This script copies all available examples for the web ui into config/code_examples. 
Add new examples to the tool_id section in the tool_examples_map dict following the rules already available.
"""

HERE = osp.abspath(osp.dirname(__file__))
PROJECT_ROOT = osp.join(HERE, '..', '..')
EXAMPLES_DIR = osp.join(PROJECT_ROOT, 'examples')


tool_examples_map = {
  'boogieAutomizer': [
    {
      'path': osp.join(EXAMPLES_DIR, 'programs', 'toy', 'showcase'),
      'pattern': '*.bpl'
    }
  ],
  'cAutomizer': [
    {
      'path': osp.join(EXAMPLES_DIR, 'programs', 'toy', 'showcase'),
      'pattern': '*.c'
    },
    {
      'path': osp.join(EXAMPLES_DIR, 'programs', 'quantifier', 'regression', 'c'),
      'pattern': 'FunctionPointers01.c'
    }
  ],
  'cBuchiAutomizer': [
    {
      'path': osp.join(EXAMPLES_DIR, 'programs', 'termination', 'showcase'),
      'pattern': '*.c'
    }
  ],
  'boogieBuchiAutomizer': [
    {
      'path': osp.join(EXAMPLES_DIR, 'programs', 'termination', 'showcase'),
      'pattern': '*.bpl'
    }
  ]
}


dest = osp.join(HERE, 'config', 'code_examples')
if not osp.exists(dest):
  os.makedirs(dest)


code_examples = collections.defaultdict(list)
for tool, examples in tool_examples_map.items():
    for example in examples:
        path = example['path']
        pattern = example['pattern']
        for file in glob.glob(rf'{path}/{pattern}'):
            shutil.copy(file, dest)
            basename = osp.basename(file)
            code_examples[tool].append({
              'name': (basename[:30] + '...') if len(basename) > 20 else basename,
              'source': basename
            })

with open(osp.join(HERE, 'config', 'code_examples', 'code_examples.json'), mode='w') as out_file:
  json.dump(code_examples, out_file)
