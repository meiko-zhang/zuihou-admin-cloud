package com.github.zuihou.file.service.impl;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.github.zuihou.common.enums.IconType;
import com.github.zuihou.common.utils.context.DozerUtils;
import com.github.zuihou.file.biz.FileBiz;
import com.github.zuihou.file.dao.FileMapper;
import com.github.zuihou.file.domain.FileAttrDO;
import com.github.zuihou.file.domain.FileDO;
import com.github.zuihou.file.domain.FileStatisticsDO;
import com.github.zuihou.file.dto.FileOverviewDTO;
import com.github.zuihou.file.dto.FileStatisticsAllDTO;
import com.github.zuihou.file.dto.FolderDTO;
import com.github.zuihou.file.dto.FolderSaveDTO;
import com.github.zuihou.file.entity.DownWater;
import com.github.zuihou.file.entity.File;
import com.github.zuihou.file.entity.Recycle;
import com.github.zuihou.file.entity.Share;
import com.github.zuihou.file.enumeration.DataType;
import com.github.zuihou.file.service.DownWaterService;
import com.github.zuihou.file.service.FileService;
import com.github.zuihou.file.service.RecycleService;
import com.github.zuihou.file.service.ShareService;
import com.github.zuihou.file.strategy.FileStrategy;
import com.github.zuihou.mybatis.conditions.Wraps;
import com.github.zuihou.mybatis.conditions.query.LbqWrapper;
import com.github.zuihou.mybatis.conditions.update.LbuWrapper;
import com.github.zuihou.utils.DateUtils;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import static com.github.zuihou.common.constant.CommonConstants.PARENT_ID_DEF;
import static com.github.zuihou.common.constant.CommonConstants.ROOT_PATH_DEF;
import static com.github.zuihou.common.excode.ExceptionCode.BASE_VALID_PARAM;
import static com.github.zuihou.utils.BizAssert.assertEquals;
import static com.github.zuihou.utils.BizAssert.assertFalse;
import static com.github.zuihou.utils.BizAssert.assertNotNull;
import static com.github.zuihou.utils.BizAssert.assertTrue;
import static java.util.stream.Collectors.groupingBy;

/**
 * <p>
 * 业务实现类
 * 文件表
 * </p>
 *
 * @author zuihou
 * @date 2019-06-24
 */
@Slf4j
@Service
public class FileServiceImpl extends ServiceImpl<FileMapper, File> implements FileService {

    @Autowired
    private DozerUtils dozerUtils;
    @Autowired
    private RecycleService recycleService;
    @Autowired
    private ShareService shareService;
    @Autowired
    private FileBiz fileBiz;
    @Resource
    private FileStrategy fileStrategy;
    @Autowired
    private DownWaterService downWaterService;

    @Override
    public File upload(MultipartFile simpleFile, Long folderId) {
        FileAttrDO fileAttrDO = this.getFileAttrDo(folderId);
        String treePath = fileAttrDO.getTreePath();
        String folderName = fileAttrDO.getFolderName();
        Integer grade = fileAttrDO.getGrade();

        File file = fileStrategy.upload(simpleFile);
        file.setFolderId(folderId);
        file.setFolderName(folderName);
        file.setGrade(grade);
        file.setTreePath(treePath);
        super.save(file);
        return file;
    }

    @Override
    public FileAttrDO getFileAttrDo(Long folderId) {
        String treePath = ROOT_PATH_DEF;
        String folderName = "";
        Integer grade = 1;
        if (folderId == null || folderId <= 0) {
            return new FileAttrDO(treePath, grade, folderName, PARENT_ID_DEF);
        }
        File folder = this.getById(folderId);

        if (folder != null && !folder.getIsDelete() && DataType.DIR.eq(folder.getDataType())) {
            folderName = folder.getSubmittedFileName();
            treePath = StringUtils.join(folder.getTreePath(), folder.getId(), ROOT_PATH_DEF);
            grade = folder.getGrade() + 1;
        }
        assertTrue(BASE_VALID_PARAM.build("文件夹层级不能超过10层"), grade <= 10);
        return new FileAttrDO(treePath, grade, folderName, folderId);
    }


    @Override
    public FolderDTO saveFolder(FolderSaveDTO folderSaveDto) {
        File folder = dozerUtils.map2(folderSaveDto, File.class);
        if (folderSaveDto.getParentId() == null || folderSaveDto.getParentId() <= 0) {
            folder.setFolderId(PARENT_ID_DEF);
            folder.setTreePath(ROOT_PATH_DEF);
            folder.setGrade(1);
        } else {
            File parent = super.getById(folderSaveDto.getParentId());
            assertNotNull(BASE_VALID_PARAM.build("父文件夹不能为空"), parent);
            assertFalse(BASE_VALID_PARAM.build("父文件夹已经被删除"), parent.getIsDelete());
            assertEquals(BASE_VALID_PARAM.build("父文件夹不存在"), DataType.DIR.name(), parent.getDataType().name());
            assertTrue(BASE_VALID_PARAM.build("文件夹层级不能超过10层"), parent.getGrade() < 10);
            folder.setFolderName(parent.getSubmittedFileName());
            folder.setTreePath(StringUtils.join(parent.getTreePath(), parent.getId(), ROOT_PATH_DEF));
            folder.setGrade(parent.getGrade() + 1);
        }
        if (folderSaveDto.getOrderNum() == null) {
            folderSaveDto.setOrderNum(0);
        }
        folder.setIsDelete(false);
        folder.setDataType(DataType.DIR);
        folder.setIcon(IconType.DIR.getIcon());
        setDate(folder);
        super.save(folder);
        return dozerUtils.map2(folder, FolderDTO.class);
    }

    private void setDate(File file) {
        LocalDateTime now = LocalDateTime.now();
        file.setCreateMonth(DateUtils.formatAsYearMonthEn(now))
                .setCreateWeek(DateUtils.formatAsYearWeekEn(now))
                .setCreateDay(DateUtils.formatAsDateEn(now));
    }

    public boolean removeFile(Long[] ids, Long userId) {
        LbuWrapper<File> lambdaUpdate =
                Wraps.<File>lbU()
                        .in(File::getId, ids)
                        .eq(File::getCreateUser, userId);
        File file = File.builder().isDelete(Boolean.TRUE).build();

        return super.update(file, lambdaUpdate);
    }

    @Override
    public void remove(Long id, Long userId) {
        File folder = super.getById(id);
        assertNotNull(BASE_VALID_PARAM.build("文件不存在，请确认是否已被删除"), folder);

        removeFile(new Long[]{id}, userId);
        File file = super.getById(id);
        Recycle recycle = dozerUtils.map(file, Recycle.class);
        recycleService.save(recycle);
    }

    @Override
    public Boolean removeList(Long userId, Long[] ids) {
        if (ArrayUtils.isEmpty(ids)) {
            return Boolean.TRUE;
        }
        removeFile(ids, userId);
        LbqWrapper<File> query = Wraps.<File>lbQ()
                .eq(File::getCreateUser, userId)
                .in(File::getId, ids);

        List<File> list = super.list(query);
        List<Recycle> recycles = dozerUtils.mapList(list, Recycle.class);

        recycleService.saveBatch(recycles);
        return Boolean.TRUE;
    }

    @Override
    public void download(HttpServletRequest request, HttpServletResponse response, Long[] ids, Long userId) throws Exception {
        if (ids == null || ids.length == 0) {
            return;
        }
        List<File> list = (List<File>) super.listByIds(Arrays.asList(ids));

        if (list == null || list.size() == 0) {
            return;
        }
        List<FileDO> listDo = list.stream().map((file) ->
                FileDO.builder()
                        .dataType(file.getDataType())
                        .size(file.getSize())
                        .submittedFileName(file.getSubmittedFileName())
                        .url(file.getUrl())
                        .build())
                .collect(Collectors.toList());
        fileBiz.down(listDo, request, response);
        recordWater(ids, userId);
    }

    /**
     * 记录流水
     *
     * @param ids    文件id
     * @param userId
     */
    public void recordWater(Long[] ids, Long userId) {
        if (userId == null || userId <= 0) {
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            List<DownWater> list = Arrays.asList(ids).stream().map((id) -> {
                DownWater dw = DownWater.builder().appCode("").fileId(id)
                        .createMonth(DateUtils.formatAsYearMonthEn(now))
                        .createWeek(DateUtils.formatAsYearWeekEn(now))
                        .createDay(DateUtils.formatAsDateEn(now))
                        .build();
                dw.setCreateUser(userId);
                return dw;
            }).collect(Collectors.toList());

            if (!list.isEmpty()) {
                downWaterService.saveBatch(list);
            }
        } catch (Exception e) {
            log.error("记录流水报错={}", e);
        }
    }

    @Override
    public FileOverviewDTO findOverview(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        InnerQueryDate innerQueryDate = new InnerQueryDate(userId, startTime, endTime).invoke();
        startTime = innerQueryDate.getStartTime();
        endTime = innerQueryDate.getEndTime();

        List<FileStatisticsDO> list = baseMapper.findNumAndSizeByUserId(userId, null, "ALL", startTime, endTime);
        FileOverviewDTO.FileOverviewDTOBuilder builder = FileOverviewDTO.myBuilder();

        long allSize = 0L;
        int allNum = 0;
        for (FileStatisticsDO fs : list) {
            allSize += fs.getSize();
            allNum += fs.getNum();
            switch (fs.getDataType()) {
                case DIR:
                    builder.dirNum(fs.getNum());
                    break;
                case IMAGE:
                    builder.imgNum(fs.getNum());
                    break;
                case VIDEO:
                    builder.videoNum(fs.getNum());
                    break;
                case DOC:
                    builder.docNum(fs.getNum());
                    break;
                case AUDIO:
                    builder.audioNum(fs.getNum());
                    break;
                case OTHER:
                    builder.otherNum(fs.getNum());
                    break;
                default:
                    break;
            }
        }
        builder.allFileNum(allNum).allFileSize(allSize);
        int shareNum = shareService.count(Wrappers.<Share>lambdaQuery().eq(Share::getCreateUser, userId));
        builder.shareNum(shareNum);
        return builder.build();
    }

    @Override
    public FileStatisticsAllDTO findAllByDate(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        InnerQueryDate innerQueryDate = new InnerQueryDate(userId, startTime, endTime).invoke();
        startTime = innerQueryDate.getStartTime();
        endTime = innerQueryDate.getEndTime();
        List<String> dateList = innerQueryDate.getDateList();
        String dateType = innerQueryDate.getDateType();

        //不完整的数据
        List<FileStatisticsDO> list = baseMapper.findNumAndSizeByUserId(userId, dateType, null, startTime, endTime);

        //按月份分类
        Map<String, List<FileStatisticsDO>> map = list.stream().collect(groupingBy(FileStatisticsDO::getDateType));

        List<Long> sizeList = new ArrayList<>();
        List<Integer> numList = new ArrayList<>();

        dateList.forEach((date) -> {
            if (map.containsKey(date)) {
                List<FileStatisticsDO> subList = map.get(date);

                Long size = subList.stream().mapToLong(FileStatisticsDO::getSize).sum();
                Integer num = subList.stream().filter((fs) -> !DataType.DIR.eq(fs.getDataType()))
                        .mapToInt(FileStatisticsDO::getNum).sum();
                sizeList.add(size);
                numList.add(num);
            } else {
                sizeList.add(0L);
                numList.add(0);
            }
        });

        return FileStatisticsAllDTO.builder().dateList(dateList).numList(numList).sizeList(sizeList).build();
    }


    @Override
    public List<FileStatisticsDO> findAllByDataType(Long userId) {
        List<DataType> dataTypes = Arrays.asList(DataType.values());
        List<FileStatisticsDO> list = baseMapper.findNumAndSizeByUserId(userId, null, "ALL", null, null);

        Map<DataType, List<FileStatisticsDO>> map = list.stream().collect(groupingBy(FileStatisticsDO::getDataType));

        return dataTypes.stream().map((type) -> {
            FileStatisticsDO fs = null;
            if (map.containsKey(type)) {
                fs = map.get(type).get(0);
            } else {
                fs = FileStatisticsDO.builder().dataType(type).size(0L).num(0).build();
            }
            return fs;
        }).collect(Collectors.toList());
    }

    @Override
    public List<FileStatisticsDO> downTop20(Long userId) {
        return baseMapper.findDownTop20(userId);
    }

    @Override
    public FileStatisticsAllDTO findNumAndSizeToTypeByDate(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
        return common(userId, startTime, endTime,
                (qd) -> baseMapper.findNumAndSizeByUserId(qd.getUserId(), qd.getDateType(), "ALL", qd.getStartTime(), qd.getEndTime()));
    }

    @Override
    public FileStatisticsAllDTO findDownSizeByDate(Long userId, LocalDateTime startTime,
                                                   LocalDateTime endTime) {
        return common(userId, startTime, endTime,
                (qd) -> baseMapper.findDownSizeByDate(qd.getUserId(), qd.getDateType(), qd.getStartTime(), qd.getEndTime()));
    }

    /**
     * 抽取公共查询公共代码
     *
     * @param userId    用户id
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param function  回调函数
     * @return
     */
    private FileStatisticsAllDTO common(Long userId, LocalDateTime startTime, LocalDateTime endTime, Function<InnerQueryDate, List<FileStatisticsDO>> function) {
        InnerQueryDate innerQueryDate = new InnerQueryDate(userId, startTime, endTime).invoke();
        List<String> dateList = innerQueryDate.getDateList();

        List<FileStatisticsDO> list = function.apply(innerQueryDate);

        //按月份分类
        Map<String, List<FileStatisticsDO>> map = list.stream().collect(groupingBy(FileStatisticsDO::getDateType));

        List<Long> sizeList = new ArrayList<>(dateList.size());
        List<Integer> numList = new ArrayList<>(dateList.size());

        List<Integer> dirNumList = new ArrayList<>(dateList.size());

        List<Long> imgSizeList = new ArrayList<>(dateList.size());
        List<Integer> imgNumList = new ArrayList<>(dateList.size());

        List<Long> videoSizeList = new ArrayList<>(dateList.size());
        List<Integer> videoNumList = new ArrayList<>(dateList.size());

        List<Long> audioSizeList = new ArrayList<>(dateList.size());
        List<Integer> audioNumList = new ArrayList<>(dateList.size());

        List<Long> docSizeList = new ArrayList<>(dateList.size());
        List<Integer> docNumList = new ArrayList<>(dateList.size());

        List<Long> otherSizeList = new ArrayList<>(dateList.size());
        List<Integer> otherNumList = new ArrayList<>(dateList.size());

        dateList.forEach((date) -> {
            if (map.containsKey(date)) {
                List<FileStatisticsDO> subList = map.get(date);

                Function<DataType, Stream<FileStatisticsDO>> stream = (dataType) -> subList.stream().filter((fs) -> !dataType.eq(fs.getDataType()));
                Long size = stream.apply(DataType.DIR).mapToLong(FileStatisticsDO::getSize).sum();
                Integer num = stream.apply(DataType.DIR).mapToInt(FileStatisticsDO::getNum).sum();
                sizeList.add(size);
                numList.add(num);

                Integer dirNum = subList.stream().filter((fs) -> DataType.DIR.eq(fs.getDataType()))
                        .mapToInt(FileStatisticsDO::getNum).sum();
                dirNumList.add(dirNum);

                add(imgSizeList, imgNumList, subList, DataType.IMAGE);
                add(videoSizeList, videoNumList, subList, DataType.VIDEO);
                add(audioSizeList, audioNumList, subList, DataType.AUDIO);
                add(docSizeList, docNumList, subList, DataType.DOC);
                add(otherSizeList, otherNumList, subList, DataType.OTHER);

            } else {
                sizeList.add(0L);
                numList.add(0);
                dirNumList.add(0);
                imgSizeList.add(0L);
                imgNumList.add(0);
                videoSizeList.add(0L);
                videoNumList.add(0);
                audioSizeList.add(0L);
                audioNumList.add(0);
                docSizeList.add(0L);
                docNumList.add(0);
                otherSizeList.add(0L);
                otherNumList.add(0);
            }
        });

        return FileStatisticsAllDTO.builder()
                .dateList(dateList)
                .numList(numList).sizeList(sizeList)
                .dirNumList(dirNumList)
                .imgNumList(imgNumList).imgSizeList(imgSizeList)
                .videoNumList(videoNumList).videoSizeList(videoSizeList)
                .audioNumList(audioNumList).audioSizeList(audioSizeList)
                .docNumList(docNumList).docSizeList(docSizeList)
                .otherNumList(otherNumList).otherSizeList(otherSizeList)
                .build();
    }

    private void add(List<Long> sizeList, List<Integer> numList, List<FileStatisticsDO> subList, DataType dt) {
        Function<DataType, Stream<FileStatisticsDO>> stream =
                dataType -> subList.stream().filter(fs -> dataType.eq(fs.getDataType()));

        Long size = stream.apply(dt).mapToLong(FileStatisticsDO::getSize).sum();
        Integer num = stream.apply(dt).mapToInt(FileStatisticsDO::getNum).sum();
        sizeList.add(size);
        numList.add(num);
    }

    @Getter
    private static class InnerQueryDate {
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private List<String> dateList;
        private String dateType;
        private Long userId;

        public InnerQueryDate(Long userId, LocalDateTime startTime, LocalDateTime endTime) {
            this.userId = userId;
            this.startTime = startTime;
            this.endTime = endTime;
        }

        public InnerQueryDate invoke() {
            if (startTime == null) {
                startTime = LocalDateTime.now().plusDays(-9);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            endTime = LocalDateTime.of(endTime.toLocalDate(), LocalTime.MAX);
            dateList = new ArrayList<>();
            dateType = DateUtils.calculationEn(startTime, endTime, dateList);
            return this;
        }
    }
}
