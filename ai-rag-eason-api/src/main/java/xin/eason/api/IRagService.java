package xin.eason.api;

import org.springframework.web.multipart.MultipartFile;
import xin.eason.api.response.Result;

import java.util.List;

/**
 * 对外提供 RAG 知识库服务的接口
 */
public interface IRagService {

    /**
     * 查询已有的 RAG 知识库的 Tag 标签
     * @return Tag 标签列表
     */
    Result<List<String>> queryRagTagList();

    /**
     * 上传 RAG 知识库文件
     * @param ragTag 上传的 RAG 知识库的 Tag 标签
     * @param files 上传的一系列文件, 使用列表封装
     * @return 上传
     */
    Result<String> uploadRagFiles(String ragTag, List<MultipartFile> files);

    /**
     * 根据传入的仓库 URL 和用户名, Token 克隆 Git 仓库, 然后上传知识库
     * @param repositoryUrl 仓库 URL
     * @param userName 用户名
     * @param token 用户 Token
     * @return 分析结果
     */
    Result<String> analyseGitRepository(String repositoryUrl, String userName, String token);
}
