import sys
import re
import os
import shutil
import commands
import fnmatch

"""ACME Model Generator
"""
def run_acme(apk_file, resources_dir):
    apk_name = os.path.basename(apk_file).replace('.','_')[:-4]
    model_name = apk_name + '.xml'
    cmd = 'java -jar ' + os.path.abspath(os.path.join(resources_dir,'acme.jar')) + ' ' + os.path.abspath(os.path.join(os.path.dirname(apk_file), model_name)) +  ' ' + os.path.abspath(os.path.dirname(apk_file)) + ' ' + os.path.abspath(os.path.join(resources_dir,'templates'))
    #print cmd
    os.system(cmd + '&> /dev/null')

def run_covert(apk_file, resources_dir):
    cmd = 'java -Xmx10G -jar ' + os.path.abspath(os.path.join(resources_dir,'acme_lib/covert.jar')) + ' -mode analyzer2 -in ' + os.path.abspath(apk_file) + ' -out ' + os.path.abspath(os.path.dirname(apk_file)) + ' -map  ' + os.path.abspath(os.path.join(resources_dir,'prmMapping/prmDomains.txt')) + ' -map_api ' + os.path.abspath(os.path.join(resources_dir,'prmMapping/jellybean_allmappings.txt'))
    #print cmd
    os.system(cmd + '&> /dev/null')

def main():
  args = sys.argv[1:]
  if not args:
    print "usage: run.py APk_File Resources_Dir"
    sys.exit(1)
  apk = args[0]
  resources = args[1]
  run_covert(apk, resources)
  run_acme(apk, resources)
  
if __name__ == "__main__":
  main()
