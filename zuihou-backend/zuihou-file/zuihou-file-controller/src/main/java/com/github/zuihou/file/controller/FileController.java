package com.github.zuihou.file.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.github.zuihou.base.BaseController;
import com.github.zuihou.base.Result;
import com.github.zuihou.base.entity.SuperEntity;
import com.github.zuihou.common.utils.context.DozerUtils;
import com.github.zuihou.file.dto.FilePageReqDTO;
import com.github.zuihou.file.dto.FileUpdateDTO;
import com.github.zuihou.file.dto.FolderDTO;
import com.github.zuihou.file.dto.FolderSaveDTO;
import com.github.zuihou.file.entity.File;
import com.github.zuihou.file.manager.FileRestManager;
import com.github.zuihou.file.service.FileService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 文件表 前端控制器
 * </p>
 *
 * @author zuihou
 * @since 2019-04-29
 */
@Validated
@RestController
@RequestMapping("/file")
@Slf4j
@Api(value = "文件表", description = "文件表")
public class FileController extends BaseController {
    @Autowired
    private FileService fileService;
    @Autowired
    private FileRestManager fileRestManager;
    @Autowired
    private DozerUtils dozerUtils;

    /**
     * 查询单个文件信息
     *
     * @param id
     * @return
     */
    @ApiOperation(value = "获取文件", notes = "获取文件")
    @GetMapping
    public Result<File> get(@RequestParam(value = "id") Long id) {
        File file = fileService.getById(id);
        if (file != null && file.getIsDelete()) {
            return Result.success(null);
        }
        return Result.success(file);
    }

    /**
     * 获取文件分页
     *
     * @author zuihou
     * @date 2019-05-06
     */
    @ApiOperation(value = "获取文件分页", notes = "获取文件分页")
    @GetMapping(value = "/page")
    @Validated(SuperEntity.OnlyQuery.class)
    public Result<IPage<File>> page(@Valid FilePageReqDTO data) {
        return Result.success(fileRestManager.page(getPage(), data));
    }

    /**
     * 上传文件
     *
     * @param
     * @return
     * @author zuihou
     * @date 2019-05-06 16:28
     */
    @ApiOperation(value = "上传文件", notes = "上传文件 ")
    @ApiResponses({
            @ApiResponse(code = 60102, message = "文件夹为空"),
    })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "folderId", value = "文件夹id", dataType = "long", paramType = "query"),
            @ApiImplicitParam(name = "file", value = "附件", dataType = "MultipartFile", allowMultiple = true, required = true),
    })
    @RequestMapping(value = "/upload", method = RequestMethod.POST)
    public Result<File> upload(@RequestParam(value = "folderId") Long folderId,
                               @RequestParam(value = "file") MultipartFile simpleFile) {
        //1，先将文件存在本地,并且生成文件名
        log.info("contentType={}, name={} , sfname={}", simpleFile.getContentType(), simpleFile.getName(), simpleFile.getOriginalFilename());
        // 忽略路径字段,只处理文件类型
        if (simpleFile.getContentType() == null) {
            return Result.fail("文件为空");
        }

        File file = fileService.upload(simpleFile, folderId);

        return Result.success(file);
    }


    /**
     * 保存文件夹
     *
     * @param
     * @return
     * @author zuihou
     * @date 2019-05-06 16:28
     */
    @ApiResponses({
            @ApiResponse(code = 60000, message = "文件夹为空"),
            @ApiResponse(code = 60001, message = "文件夹名称为空"),
            @ApiResponse(code = 60002, message = "父文件夹为空"),
    })
    @ApiOperation(value = "保存文件夹", notes = "Response Messages 中的HTTP Status Code 值的是errcode的值")
    @RequestMapping(value = "", method = RequestMethod.POST)
    public Result<FolderDTO> saveFolder(@Valid @RequestBody FolderSaveDTO folderSaveDto) {
        //2，获取身份

        FolderDTO folder = fileService.saveFolder(folderSaveDto);
        return Result.success(folder);
    }

    /**
     * 修改文件、文件夹信息
     *
     * @param fileUpdateDTO
     * @return
     */
    @ApiOperation(value = "修改文件/文件夹名称", notes = "修改文件/文件夹名称")
    @ApiResponses({
            @ApiResponse(code = 60100, message = "文件为空"),
    })
    @RequestMapping(value = "", method = RequestMethod.PUT)
    public Result<Boolean> update(@Valid @RequestBody FileUpdateDTO fileUpdateDTO) {
        // 判断文件名是否有 后缀
        if (StringUtils.isNotEmpty(fileUpdateDTO.getSubmittedFileName())) {
            File oldFile = fileService.getById(fileUpdateDTO.getId());
            if (oldFile.getExt() != null && !fileUpdateDTO.getSubmittedFileName().endsWith(oldFile.getExt())) {
                fileUpdateDTO.setSubmittedFileName(fileUpdateDTO.getSubmittedFileName() + "." + oldFile.getExt());
            }
        }
        File file = dozerUtils.map2(fileUpdateDTO, File.class);

        fileService.updateById(file);
        return Result.success(true);
    }

    /**
     * 根据Ids进行文件删除
     *
     * @param ids
     * @return
     */
    @ApiOperation(value = "根据Ids进行文件删除", notes = "根据Ids进行文件删除  ")
    @DeleteMapping(value = "/list")
    public Result<Boolean> removeList(@RequestParam(value = "ids[]") Long[] ids) {
        Long userId = getUserId();
        return Result.success(fileService.removeList(userId, ids));
    }

    /**
     * 下载一个文件或多个文件打包下载
     *
     * @param ids
     * @param response
     * @throws Exception
     */
    @ApiOperation(value = "下载一个文件或多个文件打包下载", notes = "下载一个文件或多个文件打包下载")
    @GetMapping(value = "/download", produces = "application/octet-stream")
    public void download(
            @ApiParam(name = "ids[]", value = "文件id 数组")
            @RequestParam(value = "ids[]") Long[] ids,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        fileRestManager.download(request, response, ids, null);
    }


}
