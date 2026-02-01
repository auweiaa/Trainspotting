# Project Trainspotting

---

## Team Git Workflow

### Branches
- **`main`** – stable / playable / release-ready  
- **`development`** – integration branch (new work is merged here first)  
- **`<initials>/feature/<description>`** – new features  
 



### Rules
1. **Never push directly to `main`.**  
   All changes must go through a Pull Request.

2. **Feature branches are created from `development`** and merged back into `development` via Pull Request.

3. **Merge strategy:**
   - Feature → `development`: **Merge commit**
   - `development` → `main`: **Merge commit**  


4. **Reviews:**
   - Pull Requests into `main` require **1 approval**

---

## Git Commands

Always create new work from the `development` branch to ensure you are working on the latest integrated version.


**Start a new feature**
```bash
git checkout development
git pull
git checkout -b <initials>/feature/<description>
```

**Work and push changes**
```bash
git add -A
git commit -m "<short message>"
git push -u origin <initials>/feature/<description>
```

**Clean up after merge**
```bash
git checkout development
git pull
git fetch --prune
git branch -d <initials>/feature/<description>
```
---
