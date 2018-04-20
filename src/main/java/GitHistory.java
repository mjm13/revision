import org.apache.commons.lang3.StringUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.filter.RevFilter;
import org.eclipse.jgit.treewalk.TreeWalk;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class GitHistory {
	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
	private String dirPath = "需查看的git项目下的.git文件夹路径";
	private Git git;
	private Repository repository;

	private Date begin;
	private Date end;
	private String keyword;
	private String[] keywords;

	private Set<String> commitFiles;	//依据条件查询到的提交文件记录
	private List<String> warnings;		//警告信息
	private List<String> infos;				//提交信息

	public static void main(String[] args) throws Exception {
		GitHistory gitHistory = new GitHistory();
		gitHistory.init();
		gitHistory.setKeyword("需查询的关键字");
		gitHistory.searchLog();
		System.out.println("-----------------commit info--------------------------");
		for(String info : gitHistory.getInfos()){
			System.out.println(info);
		}
		System.out.println("-----------------getWarning info--------------------------");
		for(String info : gitHistory.getWarnings()){
			System.out.println(info);
		}
		System.out.println("-----------------fileList info--------------------------");
		for(String file : gitHistory.commitFiles){
			System.out.println(file);
		}
	}

	public void init() throws IOException, GitAPIException {
		git = Git.open(new File(dirPath));
		repository = git.getRepository();
		this.infos = new ArrayList<>();
		this.warnings = new ArrayList<>();
		this.commitFiles = new HashSet<>();
	}

	private boolean need(String msg, Date commitDate) {
		boolean result = false;
		if(keywords != null){
			for (String str : keywords){
				result = StringUtils.isNotBlank(str) && msg.indexOf(str) > -1;
				if(result){
					break;
				}
			}
		}

		if (!result) {
			result = begin != null && end != null && commitDate.after(begin) && commitDate.before(end);
		}
		return result;
	}

	public void searchLog() throws IOException, GitAPIException {
		Iterable<RevCommit> all = git.log().setRevFilter(RevFilter.NO_MERGES).call();
		for (RevCommit revCommit : all) {
			String commitMsg = revCommit.getFullMessage();
			Date commitDate = new Date(revCommit.getCommitTime() * 1000l);

			if (need(commitMsg, commitDate)) {
				infos.add(String.format("提交时间:%s 提交备注:%s",sdf.format(commitDate),revCommit.getShortMessage()));
				commitFiles.addAll(getConmmitFiles(revCommit));
			}
		}
	}

	public List<String> getConmmitFiles(RevCommit commit) throws IOException, GitAPIException {
		List<String> files = new ArrayList<>();
		String commitID = commit.getName();
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

						Iterable<RevCommit> logs = git.log().addPath(filePath).setRevFilter(RevFilter.NO_MERGES).call();
						for (RevCommit log : logs) {
							Date logCommitDate = new Date(log.getCommitTime() * 1000l);
							if (logCommitDate.after(commitDate)) {
								warnings.add(String.format("提交时间:%s 提交备注:%s",sdf.format(logCommitDate),log.getShortMessage()));
								warnings.add(filePath);
								break;
							}
						}
					}
				}
			}
			tw.close();
		}
		return files;
	}

	public void setKeyword(String keyword)
	{
		if(StringUtils.isBlank(keyword)){
			return;
		}
		this.keyword = keyword;
		keywords = StringUtils.split(",");//多个查询关键字使用,分割
	}

	public void setBegin(Date begin) {
		this.begin = begin;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public void setDirPath(String dirPath) {
		this.dirPath = dirPath;
	}

	public Set<String> getCommitFiles() {
		return commitFiles;
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public List<String> getInfos() {
		return infos;
	}
}
