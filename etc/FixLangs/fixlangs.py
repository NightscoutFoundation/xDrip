import re


def FixLanguage(file_location, new_file_location):
    with open(file_location, encoding="utf-8") as f:
        content = f.readlines()
    # you may also want to remove whitespace characters like `\n` at the end of each line
    content = [x.rstrip('\n') for x in content]
    minutes_ago = None
    minute_ago = None
    space = None
    both_found = False
    new_lines = []
    for line in content:
        new_lines.append(line)
        result = re.match("(\s*)<string name=\"space_minutes_ago\">\"(.*)\"</string>", line)
        if result:
            print('found space_minutes_ago:', result[2])
            space  = result[1]
            minutes_ago = result[2]
        result = re.match("\s*<string name=\"space_minute_ago\">\"(.*)\"</string>", line)
        if result:
            print('found space_minute_ago:', result[1])
            minute_ago = result[1]
        if both_found == False and minutes_ago and minute_ago:
            new_lines.append("%s<string name=\"minutes_ago\">{0, choice,0#{0, number, integer}%s|1#{0, number, integer}%s|1&lt;{0, number, integer}%s}</string>" %(space, minutes_ago, minute_ago ,minutes_ago))
            print('apending')
            both_found = True


    with open(new_file_location, 'w', encoding="utf-8") as f:
        for item in new_lines:
            f.write("%s\n" % item)
        f.close()

import os 

arr = []
for d,r,f in os.walk("C:\\Users\\Nirit\\take3\\xDrip\\app\\src\\main\\res"):
    for file in f:
        if "strings" in file and not "activity" in file:
            arr.append(os.path.join(d,file))
        
for file in arr:
    print(file)
    FixLanguage(file, file)