import os
import json
import shutil

base_dir = r"C:\Users\devce\OneDrive\Документы\Project\create RocketNautics\src\main\resources\data\rocketnautics"
old_recipes_dir = os.path.join(base_dir, "recipes")
new_recipes_dir = os.path.join(base_dir, "recipe")

if os.path.exists(old_recipes_dir):
    if os.path.exists(new_recipes_dir):
        
        for file in os.listdir(old_recipes_dir):
            shutil.move(os.path.join(old_recipes_dir, file), os.path.join(new_recipes_dir, file))
        os.rmdir(old_recipes_dir)
    else:
        os.rename(old_recipes_dir, new_recipes_dir)

for root, dirs, files in os.walk(new_recipes_dir):
    for file in files:
        if file.endswith(".json"):
            file_path = os.path.join(root, file)
            with open(file_path, 'r', encoding='utf-8') as f:
                try:
                    data = json.load(f)
                except:
                    continue
            
            changed = False
            
            
            if "result" in data and isinstance(data["result"], dict):
                if "item" in data["result"]:
                    data["result"]["id"] = data["result"].pop("item")
                    changed = True
            
            if "results" in data and isinstance(data["results"], list):
                for res in data["results"]:
                    if isinstance(res, dict) and "item" in res:
                        res["id"] = res.pop("item")
                        changed = True
            
            
            if "type" in data:
                if data["type"] == "minecraft:shaped":
                    data["type"] = "minecraft:crafting_shaped"
                    changed = True
                elif data["type"] == "minecraft:shapeless":
                    data["type"] = "minecraft:crafting_shapeless"
                    changed = True

            if changed:
                with open(file_path, 'w', encoding='utf-8') as f:
                    json.dump(data, f, indent=2)

print("Done fixing recipes for 1.21 format.")
