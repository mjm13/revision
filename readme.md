# java访问版本控制工具git,svn获取提交历史记录
使用JGit提取GIT提交文件列表:GitHistory

使用SVNKit提取SVN提交文件列表:SVNHistory

##GitHistory关键代码
```java
		if (commitID != null && !commitID.isEmpty()) {

			Date commitDate = new Date(commit.getCommitTime() * 1000l);
			TreeWalk tw = new TreeWalk(repository);
			tw.setRecursive(true);
			tw.addTree(commit.getTree());
			for (RevCommit parent : commit.getParents()) {
				tw.addTree(parent.getTree());
			}
			while (tw.next()) {
				int similarParents = 0;
				for (int i = 1; i < tw.getTreeCount(); i++) {
					if (tw.getFileMode(i) == tw.getFileMode(0) && tw.getObjectId(0).equals(tw.getObjectId(i))) {
						similarParents++;
					}
					if (similarParents == 0) {
						String filePath = tw.getPathString();
						files.add(filePath);
						//记录提交的文件
						infos.add(filePath);
					}
				}
			}
			tw.close();
		}

```
其中原理不清楚希望牛人解释下.
出处:https://stackoverflow.com/questions/40590039/how-to-get-the-file-list-for-a-commit-with-jgit#