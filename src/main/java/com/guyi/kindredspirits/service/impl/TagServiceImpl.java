package com.guyi.kindredspirits.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.guyi.kindredspirits.model.domain.Tag;
import com.guyi.kindredspirits.service.TagService;
import com.guyi.kindredspirits.mapper.TagMapper;
import org.springframework.stereotype.Service;

/**
* @author 张仕恒
* @description 针对表【tag(标签表)】的数据库操作Service实现
* @createDate 2023-09-26 21:17:58
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

}




