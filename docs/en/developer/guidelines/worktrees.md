# Working with Git worktrees

A Git worktree is a separate working folder for a single branch. Worktrees share a Git repository, but each has its own files and commits. In WebJET CMS, you can work on multiple tasks simultaneously, each in its own VS Code window, without switching branches and putting aside work in progress.

**Always leave the original folder `webjetcms` on branch `main`.** It is used for updating `main` and managing worktrees. Open a separate worktree for each task and do not switch branches within it.

## Preparation

- Have a working project in the **`webjetcms`** folder according to the [Installation and Startup](../install/README.md) instructions.
- Run commands in the terminal with `Bash, Git, Node.js, npm` and the available command `code` to open VS Code.
- Install the **Peacock** extension in VS Code so that the windows of individual worktrees are color-coded.

**Open the **Command Palette** with **Cmd+Shift+P** on macOS or **Ctrl+Shift+P** on Windows/Linux. Type the name of the command, for example `Git: Pull`, and press Enter. Below we use the English names of VS Code commands.

## Opening a branch

In VS Code, open the original folder **`webjetcms`**. In the bottom left, check out the branch **`main`**, open the command palette, and select **Git: Pull**.

Terminal alternative that will only allow update without merge commit:

```shell
git pull --ff-only
```

Then, via **Terminal → New Terminal**, open a terminal at the root of the original `webjetcms`. If the branch **already exists** locally or on GitHub, run:

```shell
./worktree-add.sh feature/123-fix-menu
```

If you are starting a **new branch**, use: instead

```shell
./worktree-add.sh --new feature/123-fix-menu
```

Replace the name with your branch in the form `feature/ID-short-description`. You copy it from an existing pull request in the header on GitHub. The `--new` parameter creates a branch from the current `origin/main` downloaded from the server.

The script will prepare the `../webjetcms-feature-123-fix-menu` folder, copy the settings and necessary files from `webjetcms`, install dependencies, build the administration, and open VS Code. The first run will take a while. Confirm the automatic tasks by clicking **Allow** to open the prepared terminals.

**Even after closing all of VS Code, open the worktree via the original `webjetcms` with the same command without `--new`, not by directly opening the folder.** When asked about copying local files, answer **N** or press Enter. The prepared workspace will open. Answering `Y` would overwrite its local files with copies from `webjetcms`.

## Work and update from main

Edit and save files in the task window. Submit changes via VS Code:

1. Open **Source Control**, review the changes, and click **+** on the desired files to prepare them for commit (**Stage Changes**).
2. In the **Message** field, type a description of the change and select **Git: Commit** in the command palette.
3. For the first push of a new branch, choose **Git: Publish Branch...** and optionally choose **origin**. For subsequent pushes, use **Git: Push**.

Terminal alternative for first submitting a new branch:

```shell
git push -u origin feature/123-fix-menu
```

When you need to add changes from `main` to your branch:

1. In the worktree, commit the changes in progress according to the procedure above.
2. In the original `webjetcms` window on branch `main`, open the command palette and select **Git: Pull**.
3. Return to the worktree window, press **Cmd+Shift+P** (Windows/Linux **Ctrl+Shift+P**), select **Git: Merge...** and select **main** from the list.
4. If conflicts arise, resolve them in **Source Control → Merge Changes**, mark the modified files with **+** and complete the merge with **Git: Commit**.

Terminal alternative: in the original `webjetcms` run `git pull --ff-only`, then in the worktree `git merge main`.

The updated branch `main` is common to all worktrees, you can only transfer its changes to your branch via merge.

In Codex, add each worktree folder as a **separate project** and allow access to it. Name it after the task, like `123-fix-menu`, to easily find related chats.

## Concurrent launch and tests

In VS Code, select a profile in **Run and Debug** and press **F5**. Concurrently running worktrees must use different profiles:

| Profile | HTTPS port | HTTP port |
| --- | --- | --- |
| `Debug Local DB` | 443 | 80 |
| `Debug Local DB 8443` | 8443 | 8080 |
| `Debug Local DB 9443` | 9443 | 9080 |

In the file `/etc/hosts` (in Windows `C:\Windows\System32\drivers\etc\hosts`), add:

```text
127.0.0.1 iwcm8443.interway.sk iwcm9443.interway.sk iwcm8080.interway.sk iwcm9080.interway.sk
```

In your browser, use the hostname by port, for example `https://iwcm8443.interway.sk:8443/admin/`. You will distinguish between projects and their login cookies, which are not separated by the port itself. The file `hosts` does not set the port, so include it in the URL as well.

In Codex without HTTPS certificate use `http://iwcm8080.interway.sk:8080/admin/` or `http://iwcm9080.interway.sk:9080/admin/` according to profile. [Run from terminal](../install/README.md#concurrent-run-in-multiple-worktrees) allows to set same ports.

Also point your tests to the correct port: in `src/test/webapp`, run `npm run all:8443` or `npm run all:9443` for example. These scripts use hostname `iwcm.interway.sk` with the appropriate port.

Worktrees with the same database configuration copied work on the same data.

## Deleting a completed worktree

1. In the worktree window, save your changes via **Git: Commit** and push them via **Git: Push** (a new branch via **Git: Publish Branch...**). Also set aside any local files that Git ignores and that you want to keep.
2. Stop its server, tests, and any `npm run watch`, then close its VS Code window.
3. In the original **`webjetcms` window on the `main`** branch, open **Terminal → New Terminal** and from the root of the project run:

   ```shell
   ./worktree-close.sh feature/123-fix-menu
   ```

4. Check the displayed list and confirm the deletion with a capital **Y**.

The script will delete the folder including the ignored files, VS Code workspace, and **local branch**. It will keep the branch on GitHub. It will check for uncommitted and untracked files and whether there are commits in the tracked remote branch (or in `main` if tracking is not set).
