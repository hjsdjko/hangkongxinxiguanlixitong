
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 客户
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/kehu")
public class KehuController {
    private static final Logger logger = LoggerFactory.getLogger(KehuController.class);

    @Autowired
    private KehuService kehuService;


    @Autowired
    private TokenService tokenService;
    @Autowired
    private DictionaryService dictionaryService;

    //级联表service



    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("客户".equals(role))
            params.put("kehuId",request.getSession().getAttribute("userId"));
        if(params.get("orderBy")==null || params.get("orderBy")==""){
            params.put("orderBy","id");
        }
        PageUtils page = kehuService.queryPage(params);

        //字典表数据转换
        List<KehuView> list =(List<KehuView>)page.getList();
        for(KehuView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        KehuEntity kehu = kehuService.selectById(id);
        if(kehu !=null){
            //entity转view
            KehuView view = new KehuView();
            BeanUtils.copyProperties( kehu , view );//把实体数据重构到view中

            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody KehuEntity kehu, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,kehu:{}",this.getClass().getName(),kehu.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");

        Wrapper<KehuEntity> queryWrapper = new EntityWrapper<KehuEntity>()
            .eq("username", kehu.getUsername())
            .or()
            .eq("kehu_phone", kehu.getKehuPhone())
            .or()
            .eq("kehu_id_number", kehu.getKehuIdNumber())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        KehuEntity kehuEntity = kehuService.selectOne(queryWrapper);
        if(kehuEntity==null){
            kehu.setCreateTime(new Date());
            kehu.setPassword("123456");
            kehu.setKehuSumJifen(0.0);
            kehu.setHuiyuandengjiTypes(1);
            kehuService.insert(kehu);
            return R.ok();
        }else {
            return R.error(511,"账户或者客户手机号或者客户身份证号已经被使用");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody KehuEntity kehu, HttpServletRequest request){
        logger.debug("update方法:,,Controller:{},,kehu:{}",this.getClass().getName(),kehu.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
        //根据字段查询是否有相同数据
        Wrapper<KehuEntity> queryWrapper = new EntityWrapper<KehuEntity>()
            .notIn("id",kehu.getId())
            .andNew()
            .eq("username", kehu.getUsername())
            .or()
            .eq("kehu_phone", kehu.getKehuPhone())
            .or()
            .eq("kehu_id_number", kehu.getKehuIdNumber())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        KehuEntity kehuEntity = kehuService.selectOne(queryWrapper);
        if("".equals(kehu.getKehuPhoto()) || "null".equals(kehu.getKehuPhoto())){
                kehu.setKehuPhoto(null);
        }
        if(kehuEntity==null){
            kehuService.updateById(kehu);//根据id更新
            return R.ok();
        }else {
            return R.error(511,"账户或者客户手机号或者客户身份证号已经被使用");
        }
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        kehuService.deleteBatchIds(Arrays.asList(ids));
        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            List<KehuEntity> kehuList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("../../upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            KehuEntity kehuEntity = new KehuEntity();
//                            kehuEntity.setUsername(data.get(0));                    //账户 要改的
//                            //kehuEntity.setPassword("123456");//密码
//                            kehuEntity.setKehuName(data.get(0));                    //客户姓名 要改的
//                            kehuEntity.setKehuPhone(data.get(0));                    //客户手机号 要改的
//                            kehuEntity.setKehuIdNumber(data.get(0));                    //客户身份证号 要改的
//                            kehuEntity.setKehuPhoto("");//详情和图片
//                            kehuEntity.setSexTypes(Integer.valueOf(data.get(0)));   //性别 要改的
//                            kehuEntity.setKehuEmail(data.get(0));                    //电子邮箱 要改的
//                            kehuEntity.setNewMoney(data.get(0));                    //余额 要改的
//                            kehuEntity.setKehuSumJifen(data.get(0));                    //总积分 要改的
//                            kehuEntity.setKehuNewJifen(data.get(0));                    //现积分 要改的
//                            kehuEntity.setHuiyuandengjiTypes(Integer.valueOf(data.get(0)));   //会员等级 要改的
//                            kehuEntity.setCreateTime(date);//时间
                            kehuList.add(kehuEntity);


                            //把要查询是否重复的字段放入map中
                                //账户
                                if(seachFields.containsKey("username")){
                                    List<String> username = seachFields.get("username");
                                    username.add(data.get(0));//要改的
                                }else{
                                    List<String> username = new ArrayList<>();
                                    username.add(data.get(0));//要改的
                                    seachFields.put("username",username);
                                }
                                //客户手机号
                                if(seachFields.containsKey("kehuPhone")){
                                    List<String> kehuPhone = seachFields.get("kehuPhone");
                                    kehuPhone.add(data.get(0));//要改的
                                }else{
                                    List<String> kehuPhone = new ArrayList<>();
                                    kehuPhone.add(data.get(0));//要改的
                                    seachFields.put("kehuPhone",kehuPhone);
                                }
                                //客户身份证号
                                if(seachFields.containsKey("kehuIdNumber")){
                                    List<String> kehuIdNumber = seachFields.get("kehuIdNumber");
                                    kehuIdNumber.add(data.get(0));//要改的
                                }else{
                                    List<String> kehuIdNumber = new ArrayList<>();
                                    kehuIdNumber.add(data.get(0));//要改的
                                    seachFields.put("kehuIdNumber",kehuIdNumber);
                                }
                        }

                        //查询是否重复
                         //账户
                        List<KehuEntity> kehuEntities_username = kehuService.selectList(new EntityWrapper<KehuEntity>().in("username", seachFields.get("username")));
                        if(kehuEntities_username.size() >0 ){
                            ArrayList<String> repeatFields = new ArrayList<>();
                            for(KehuEntity s:kehuEntities_username){
                                repeatFields.add(s.getUsername());
                            }
                            return R.error(511,"数据库的该表中的 [账户] 字段已经存在 存在数据为:"+repeatFields.toString());
                        }
                         //客户手机号
                        List<KehuEntity> kehuEntities_kehuPhone = kehuService.selectList(new EntityWrapper<KehuEntity>().in("kehu_phone", seachFields.get("kehuPhone")));
                        if(kehuEntities_kehuPhone.size() >0 ){
                            ArrayList<String> repeatFields = new ArrayList<>();
                            for(KehuEntity s:kehuEntities_kehuPhone){
                                repeatFields.add(s.getKehuPhone());
                            }
                            return R.error(511,"数据库的该表中的 [客户手机号] 字段已经存在 存在数据为:"+repeatFields.toString());
                        }
                         //客户身份证号
                        List<KehuEntity> kehuEntities_kehuIdNumber = kehuService.selectList(new EntityWrapper<KehuEntity>().in("kehu_id_number", seachFields.get("kehuIdNumber")));
                        if(kehuEntities_kehuIdNumber.size() >0 ){
                            ArrayList<String> repeatFields = new ArrayList<>();
                            for(KehuEntity s:kehuEntities_kehuIdNumber){
                                repeatFields.add(s.getKehuIdNumber());
                            }
                            return R.error(511,"数据库的该表中的 [客户身份证号] 字段已经存在 存在数据为:"+repeatFields.toString());
                        }
                        kehuService.insertBatch(kehuList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }


    /**
    * 登录
    */
    @IgnoreAuth
    @RequestMapping(value = "/login")
    public R login(String username, String password, String captcha, HttpServletRequest request) {
        KehuEntity kehu = kehuService.selectOne(new EntityWrapper<KehuEntity>().eq("username", username));
        if(kehu==null || !kehu.getPassword().equals(password))
            return R.error("账号或密码不正确");
        //  // 获取监听器中的字典表
        // ServletContext servletContext = ContextLoader.getCurrentWebApplicationContext().getServletContext();
        // Map<String, Map<Integer, String>> dictionaryMap= (Map<String, Map<Integer, String>>) servletContext.getAttribute("dictionaryMap");
        // Map<Integer, String> role_types = dictionaryMap.get("role_types");
        // role_types.get(.getRoleTypes());
        String token = tokenService.generateToken(kehu.getId(),username, "kehu", "客户");
        R r = R.ok();
        r.put("token", token);
        r.put("role","客户");
        r.put("username",kehu.getKehuName());
        r.put("tableName","kehu");
        r.put("userId",kehu.getId());
        return r;
    }

    /**
    * 注册
    */
    @IgnoreAuth
    @PostMapping(value = "/register")
    public R register(@RequestBody KehuEntity kehu){
//    	ValidatorUtils.validateEntity(user);
        Wrapper<KehuEntity> queryWrapper = new EntityWrapper<KehuEntity>()
            .eq("username", kehu.getUsername())
            .or()
            .eq("kehu_phone", kehu.getKehuPhone())
            .or()
            .eq("kehu_id_number", kehu.getKehuIdNumber())
            ;
        KehuEntity kehuEntity = kehuService.selectOne(queryWrapper);
        if(kehuEntity != null)
            return R.error("账户或者客户手机号或者客户身份证号已经被使用");
        kehu.setNewMoney(0.0);
        kehu.setKehuSumJifen(0.0);
        kehu.setKehuNewJifen(0.0);
        kehu.setHuiyuandengjiTypes(1);
        kehu.setCreateTime(new Date());
        kehuService.insert(kehu);
        return R.ok();
    }

    /**
     * 重置密码
     */
    @GetMapping(value = "/resetPassword")
    public R resetPassword(Integer  id){
        KehuEntity kehu = new KehuEntity();
        kehu.setPassword("123456");
        kehu.setId(id);
        kehuService.updateById(kehu);
        return R.ok();
    }


    /**
     * 忘记密码
     */
    @IgnoreAuth
    @RequestMapping(value = "/resetPass")
    public R resetPass(String username, HttpServletRequest request) {
        KehuEntity kehu = kehuService.selectOne(new EntityWrapper<KehuEntity>().eq("username", username));
        if(kehu!=null){
            kehu.setPassword("123456");
            boolean b = kehuService.updateById(kehu);
            if(!b){
               return R.error();
            }
        }else{
           return R.error("账号不存在");
        }
        return R.ok();
    }


    /**
    * 获取用户的session用户信息
    */
    @RequestMapping("/session")
    public R getCurrKehu(HttpServletRequest request){
        Integer id = (Integer)request.getSession().getAttribute("userId");
        KehuEntity kehu = kehuService.selectById(id);
        if(kehu !=null){
            //entity转view
            KehuView view = new KehuView();
            BeanUtils.copyProperties( kehu , view );//把实体数据重构到view中

            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }
    }


    /**
    * 退出
    */
    @GetMapping(value = "logout")
    public R logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return R.ok("退出成功");
    }




    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        // 没有指定排序字段就默认id倒序
        if(StringUtil.isEmpty(String.valueOf(params.get("orderBy")))){
            params.put("orderBy","id");
        }
        PageUtils page = kehuService.queryPage(params);

        //字典表数据转换
        List<KehuView> list =(List<KehuView>)page.getList();
        for(KehuView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段
        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        KehuEntity kehu = kehuService.selectById(id);
            if(kehu !=null){


                //entity转view
                KehuView view = new KehuView();
                BeanUtils.copyProperties( kehu , view );//把实体数据重构到view中

                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody KehuEntity kehu, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,kehu:{}",this.getClass().getName(),kehu.toString());
        Wrapper<KehuEntity> queryWrapper = new EntityWrapper<KehuEntity>()
            .eq("username", kehu.getUsername())
            .or()
            .eq("kehu_phone", kehu.getKehuPhone())
            .or()
            .eq("kehu_id_number", kehu.getKehuIdNumber())
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        KehuEntity kehuEntity = kehuService.selectOne(queryWrapper);
        if(kehuEntity==null){
            kehu.setCreateTime(new Date());
        kehu.setPassword("123456");
        kehuService.insert(kehu);
            return R.ok();
        }else {
            return R.error(511,"账户或者客户手机号或者客户身份证号已经被使用");
        }
    }


}
