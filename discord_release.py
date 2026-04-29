import os
import subprocess
import json
import glob
from urllib.request import Request, urlopen
from urllib.parse import urljoin
import discord_config

def get_git_info():
    try:
        # Get current branch
        branch = subprocess.check_output(["git", "rev-parse", "--abbrev-ref", "HEAD"]).decode().strip()
        # Get latest commit hashes and messages
        log = subprocess.check_output(["git", "log", "-n", "3", "--pretty=format:%h|%s|%an"]).decode().strip()
        commits = [line.split('|') for line in log.split('\n')]
        # Get hash range for comparison (current vs 3 commits ago)
        latest_hash = commits[0][0]
        oldest_hash = subprocess.check_output(["git", "rev-parse", "--short", "HEAD~3"]).decode().strip()
        
        # Detect Repository URL
        remote_url = subprocess.check_output(["git", "remote", "get-url", "origin"]).decode().strip()
        # Convert SSH to HTTPS if needed and remove .git
        if remote_url.startswith("git@"):
            remote_url = remote_url.replace(":", "/").replace("git@", "https://")
        if remote_url.endswith(".git"):
            remote_url = remote_url[:-4]
            
        return branch, commits, f"{oldest_hash}...{latest_hash}", remote_url
    except Exception as e:
        print(f"Git error: {e}")
        return "main", [], "", ""

def send_webhook(payload, files=None):
    boundary = '----DiscordBoundary'
    data = []
    
    if files:
        # Multipart form-data for files
        for name, filepath in files.items():
            with open(filepath, 'rb') as f:
                file_content = f.read()
            data.append(f'--{boundary}')
            data.append(f'Content-Disposition: form-data; name="{name}"; filename="{os.path.basename(filepath)}"')
            data.append('Content-Type: application/octet-stream')
            data.append('')
            data.append(file_content)
        
        if payload:
            data.append(f'--{boundary}')
            data.append('Content-Disposition: form-data; name="payload_json"')
            data.append('Content-Type: application/json')
            data.append('')
            data.append(json.dumps(payload))
            
        data.append(f'--{boundary}--')
        data.append('')
        
        # Join data with properly encoded line endings
        body = b'\r\n'.join([d if isinstance(d, bytes) else d.encode('utf-8') for d in data])
        headers = {
            'Content-Type': f'multipart/form-data; boundary={boundary}',
            'User-Agent': 'Mozilla/5.0'
        }
    else:
        # Simple JSON
        body = json.dumps(payload).encode('utf-8')
        headers = {
            'Content-Type': 'application/json',
            'User-Agent': 'Mozilla/5.0'
        }

    req = Request(discord_config.WEBHOOK_URL, data=body, headers=headers, method='POST')
    with urlopen(req) as res:
        return res.status

def main():
    print("🚀 Starting Discord Release...")
    branch, commits, hash_range, repo_url = get_git_info()
    
    if not repo_url:
        print("❌ Could not detect GitHub Repository URL from git origin!")
        return

    # Build the changelog
    changelog_lines = []
    for h, msg, author in commits:
        url = f"{repo_url}/commit/{h}"
        changelog_lines.append(f"- [`{h}`]({url}) {msg} - by {author}")
    
    compare_url = f"{repo_url}/compare/{hash_range}"
    
    # 1. Send the Info Embed
    payload = {
        "embeds": [{
            "title": f"Building mod from branch {branch}",
            "color": 2303786,
            "description": f"🔗 **GitHub Repository:** {repo_url}\n\n[{hash_range}]({compare_url})\n" + "\n".join(changelog_lines) + "\n\n**Build attached below**"
        }]
    }
    
    print("Sending info card...")
    send_webhook(payload)
    
    # 2. Find and Send the Jar
    jars = glob.glob("build/libs/*.jar")
    if not jars:
        print("❌ No jar files found in build/libs!")
        return
    
    latest_jar = max(jars, key=os.path.getmtime)
    print(f"Sending file: {latest_jar}...")
    send_webhook(None, files={'file': latest_jar})
    
    print("✅ Done!")

if __name__ == "__main__":
    main()
