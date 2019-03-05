''' This program  gets as input a file with strings to create and converts them
  to the correct format.
  For example, if input is:
  "Please Allow Permission"
  Location permission needed to use Bluetooth!

  The output will be:
  
  <string name="please_allow_permission">Please Allow Permission</string>
  <string name="location_permission_needed_to_use_bluetooth">Location permission needed to use Bluetooth!</string>
  
  The program will also go over all java files and replace the string with 'getApplicationContext().getString(R.string.%s)'
'''

import sys
import os

def AddImport(file_name):
    import_str = "import static com.eveningoutpost.dexdrip.xdrip.gs;\n"
    with open(file_name) as f:
        lines = f.readlines()
    
    # The code below assumes that imports have already been sorted.
    replaced = False
    with open(file_name, "w") as f:
        for line in lines:
            if import_str == line:
                continue;
            if import_str > line or  line.startswith("package") or replaced:
                f.write(line)
                continue
                
            f.write(import_str)
            replaced = True
            f.write(line)
    
     

def ReplaceString(file_name, id, string):
    content = open(file_name).read()
    full_string = '"%s"' %string
    new_string = 'gs(R.string.%s)' % id
    print('replacing ', full_string, new_string)
    if full_string in content:
        print('yeeeeeeeee')
    content = content.replace(full_string, new_string)
    file = open(file_name , "w")
    file.write(content)
    file.close()

def FileContainsString(file, string):
    #print(file)
    full_string = '"%s"' %string
    if full_string in open(file).read():
        return True
    return False

def FindFileContaingString(id, string):
    arr = []
    for d,r,f in os.walk("..\\..\\"):
        for file in f:
            if file.endswith("java") and "generated" not in file and not "PebbleDisplay" in file :
                arr.append(os.path.join(d,file))
            
    for file in arr:
        if file.startswith("..\\..\\wear"):
            continue
        if not FileContainsString(file, string): 
            continue
        print(file)
        ReplaceString(file, id, string)
        AddImport(file)


def ReadFile(file_name):
    with open(file_name) as f:
        content = f.readlines()
    # you may also want to remove whitespace characters like `\n` at the end of each line
    content = [x.strip() for x in content] 
    for line in content:
        if line.strip() == '':
            continue
        if line.startswith("#"):
            continue            
        if line.startswith('"') and line.endswith('"'):
            line = line[1:-1]
        
        header = line.lower().replace(' ','_')
        header = header.replace('\\n','_')
        header = header.replace('!','')
        header = header.replace(',','')
        header = header.replace(':','')
        header = header.replace('?','')
        header = header.replace('.','')
        header = header.replace('+','')
        header = header.replace('-','')
        header = header.replace('(','')
        header = header.replace(')','')
        header = header.replace("'",'')
        
        print ('    <string name="',header,'">', line,'</string>', sep='')
        
        FindFileContaingString(header, line)
        
        
ReadFile(sys.argv[1])