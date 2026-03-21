import re

path = '/home/rishi/Downloads/Complaint-frontend/pages/login-v2.html'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Remove `position: relative;` from `form-group` inline styles so it doesn't conflict
content = content.replace('class="form-group" style="margin: 0; position: relative;"', 'class="form-group" style="margin: 0;"')

# 2. We'll find each <label> block starting to </div>.
# Because regex is tricky with nested HTML, we'll iterate with find()
for field_id in ['name', 'signup-email', 'signup-password', 'login-email', 'login-password']:
    # Instead of regex, just find the input and wrap its siblings
    pass

# Wait, let's just do targeted string replacements Since there are 5 identical structures.
import sys

def modify_field(label_end, field_end):
    # This is fragile. We will use a reliable regex finding blocks.
    pass

# We can rely on specific pattern:
# <label ...> ... </label> \s* <svg style="position: absolute; ... top: 40px; ..."> ... </svg> \s* <input ...> [\s* <button ...</button>]?
pattern = r'(<label.*?<\/label>)\s*(<svg\s+style="position:\s*absolute;\s*left:\s*1[24]px;\s*top:\s*4[01]px;.*?>.*?<\/svg>)\s*(<input.*?>)(?:\s*(<button.*?>.*?<\/button>))?'

def replacer(m):
    label = m.group(1)
    svg = m.group(2)
    inp = m.group(3)
    btn = m.group(4) or ''
    # remove top inline style
    svg = re.sub(r'top:\s*4[01]px;?', '', svg)
    # also remove from button if present
    if btn:
        btn = re.sub(r'top:\s*4[01]px;?', '', btn)
        # we also need to center the button in the wrapper
        btn = btn.replace('background: none;', 'background: none; top: 50%; transform: translateY(-50%);')
    
    # center the svg in the wrapper
    svg = svg.replace('color:', 'top: 50%; transform: translateY(-50%); color:')
    
    return f'{label}\n<div class="input-wrapper">\n{svg}\n{inp}\n{btn}\n</div>'

new_content = re.sub(pattern, replacer, content, flags=re.DOTALL)

with open(path, 'w', encoding='utf-8') as f:
    f.write(new_content)

