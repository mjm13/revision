import org.apache.commons.lang3.StringUtils;
import org.tmatesoft.svn.core.ISVNLogEntryHandler;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import java.text.SimpleDateFormat;
import java.util.*;

public class SVNHistory {

    private  String url = "SVN地址";
    private  String name = "用户名";
    private  String pwd = "密码";

    private Date begin;
    private Date end;
    private String keyword;
    private String[] keywords;

    private Set<String> commitFiles;	//依据条件查询到的提交文件记录
    private List<String> warnings;		//警告信息
    private List<String> infos;				//提交信息

    private static SVNRepository repository = null;
    public SimpleDateFormat sdf  = null;

    public static void main(String[] args) throws Exception{
        SVNHistory svnHistory = new SVNHistory();
        svnHistory.init();
        svnHistory.setKeyword("流程节点添加人员");
        svnHistory.searchLog();
        System.out.println("-----------------commit info--------------------------");
        for(String info : svnHistory.getInfos()){
            System.out.println(info);
        }
        System.out.println("-----------------getWarning info--------------------------");
        for(String info : svnHistory.getWarnings()){
            System.out.println(info);
        }
        System.out.println("-----------------fileList info--------------------------");
        for(String file : svnHistory.commitFiles){
            System.out.println(file);
        }
    }


    public  void init() {
        DAVRepositoryFactory.setup();
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
        try {
            repository = SVNRepositoryFactory.create(SVNURL
                    .parseURIEncoded(url));
        } catch (SVNException e) {
            e.printStackTrace();
        }
        // 身份验证
        ISVNAuthenticationManager authManager = SVNWCUtil
                .createDefaultAuthenticationManager(this.name,
                        this.pwd.toCharArray());
        repository.setAuthenticationManager(authManager);

        sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm");
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

    /**
     * 按照条件过滤SVN提交记录
     *
     */
    public void searchLog() throws Exception{
        final Set<SVNLogEntry> logs = new LinkedHashSet<SVNLogEntry>();
        try {
            repository.log(new String[] {}, 0, -1, true, true,
                    new ISVNLogEntryHandler() {// 过滤提取文件
                        public void handleLogEntry(SVNLogEntry svnlogentry)
                                throws SVNException {
                            Date commitDate = svnlogentry.getDate();// 提交日期
                            String commitMsg = svnlogentry.getMessage();// 提交备注
                            if (need(commitMsg,commitDate)){
                                infos.add(String.format("提交时间:%s 提交备注:%s",sdf.format(commitDate),commitMsg));
                                Set<String> keys = svnlogentry.getChangedPaths().keySet();
                                commitFiles.addAll(keys);
                                infos.addAll(keys);
                                logs.add(svnlogentry);
                            }
                        }
                    });
            for (SVNLogEntry log : logs) {
                final Date oldCommitDate = log.getDate();// 提交日期
                Set<String> keys = log.getChangedPaths().keySet();
                repository.log(keys.toArray(new String[keys.size()]), log.getRevision(), -1, true, true,
                        new ISVNLogEntryHandler() {// 过滤提取文件
                            public void handleLogEntry(SVNLogEntry svnlogentry)
                                    throws SVNException {
                                String fileCommitMsg = svnlogentry.getMessage();// 提交备注
                                Date fileCommitDate = svnlogentry.getDate();// 提交日期
                                if (fileCommitDate.after(oldCommitDate)) {
                                    warnings.add(String.format("提交时间:%s 提交备注:%s",sdf.format(fileCommitDate),fileCommitMsg));
                                    warnings.addAll(svnlogentry.getChangedPaths().keySet());
                                }
                            }
                        });
            }
        } catch (Exception e) {
            throw new Exception("提取代码错误！",e);
        }
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setName(String name) {
        this.name = name;
    }

   public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public void setBegin(Date begin) {
        this.begin = begin;
    }

    public void setEnd(Date end) {
        this.end = end;
    }

    public void setKeyword(String keyword) {
        if(StringUtils.isBlank(keyword)){
            return;
        }
        this.keyword = keyword;
        keywords = StringUtils.split(keyword,",");//多个查询关键字使用,分割
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
