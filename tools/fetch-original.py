"""Prepare the external original without redistributing it in this repository."""
from pathlib import Path
import argparse
import hashlib
import json
import shutil
import urllib.request

ROOT=Path(__file__).resolve().parent.parent
URL='https://rgm.games/api/roms/36.swf'
SHA256='ced3849a90a97368279dec93cc06745a8484cb26b881753b90f25a08275e6c6e'
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--input',type=Path,help='Use an already obtained original instead of downloading it')
args=parser.parse_args()
target=ROOT/'swf/original/game.swf'
target.parent.mkdir(parents=True,exist_ok=True)
temporary=target.with_suffix('.partial')
if target.exists() and hashlib.sha256(target.read_bytes()).hexdigest()==SHA256:
    print('Verified original already present.');raise SystemExit(0)
try:
    if args.input:
        shutil.copyfile(args.input,temporary)
    else:
        request=urllib.request.Request(URL,headers={'User-Agent':'DadNMe-LAN-Coop-source-bootstrap/0.5'})
        with urllib.request.urlopen(request,timeout=40) as response,temporary.open('wb') as output:
            shutil.copyfileobj(response,output)
    digest=hashlib.sha256(temporary.read_bytes()).hexdigest()
    if digest!=SHA256:raise RuntimeError('Original checksum differs; no game patch was applied.')
    temporary.replace(target)
    print(json.dumps({'source':URL,'sha256':digest,'bytes':target.stat().st_size}))
finally:
    temporary.unlink(missing_ok=True)
