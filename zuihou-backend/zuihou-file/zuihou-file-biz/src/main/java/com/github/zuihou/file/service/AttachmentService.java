package com.github.zuihou.file.service;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.baomidou.mybatisplus.extension.service.IService;
import com.github.zuihou.file.dto.AttachmentDTO;
import com.github.zuihou.file.dto.AttachmentResultDTO;
import com.github.zuihou.file.entity.Attachment;

import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 业务接口
 * 附件
 * </p>
 *
 * @author zuihou
 * @date 2019-06-24
 */
public interface AttachmentService extends IService<Attachment> {
    /**
     * 上传附件
     *
     * @param file
     * @param appCode
     * @param id
     * @param bizType
     * @param bizId
     * @return
     */
    AttachmentDTO upload(MultipartFile file, String appCode, Long id, String bizType, String bizId);

    /**
     * 删除附件
     *
     * @param ids
     */
    void remove(Long[] ids);

    /**
     * 根据业务id和业务类型删除附件
     *
     * @param bizId
     * @param bizType
     */
    void removeByBizIdAndBizType(String bizId, String bizType);

    /**
     * 根据业务类型和业务id查询附件
     *
     * @param bizTypes
     * @param bizIds
     * @return
     */
    List<AttachmentResultDTO> find(String[] bizTypes, String[] bizIds);

    /**
     * 根据文件id下载附件
     *
     * @param request
     * @param response
     * @param ids
     * @throws Exception
     */
    void download(HttpServletRequest request, HttpServletResponse response, Long[] ids) throws Exception;

    /**
     * 根据业务id和业务类型下载附件
     *
     * @param request
     * @param response
     * @param bizTypes
     * @param bizIds
     * @throws Exception
     */
    void downloadByBiz(HttpServletRequest request, HttpServletResponse response, String[] bizTypes, String[] bizIds) throws Exception;

    /**
     * 根据文件url下载附件
     *
     * @param request
     * @param response
     * @param url
     * @param filename
     * @throws Exception
     */
    void downloadByUrl(HttpServletRequest request, HttpServletResponse response, String url, String filename) throws Exception;

}
