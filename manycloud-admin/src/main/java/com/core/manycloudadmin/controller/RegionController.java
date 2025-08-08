package com.core.manycloudadmin.controller;

import com.core.manycloudadmin.service.RegionService;
import com.core.manycloudadmin.so.BaseDelByIdSO;
import com.core.manycloudadmin.so.admin.*;
import com.core.manycloudadmin.so.region.*;
import com.core.manycloudcommon.controller.BaseController;
import com.core.manycloudcommon.utils.ResultMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/region")
public class RegionController extends BaseController {

    @Autowired
    private RegionService regionService;

    /**
     * 添加大洲信息
     * @param addContinentSO
     * @return
     */
    @PostMapping("/add/continent")
    public ResultMessage addContinent(@RequestBody AddContinentSO addContinentSO){
        return regionService.addContinent(addContinentSO);
    }


    /**
     * 更新大洲信息
     * @param updateContinentSO
     * @return
     */
    @PostMapping("/update/continent")
    public ResultMessage updateContinent(@RequestBody UpdateContinentSO updateContinentSO){
        return regionService.updateContinent(updateContinentSO);
    }


    /**
     * 删除大洲信息
     * @param delByIdSO
     * @return
     */
    @PostMapping("/del/continent")
    public ResultMessage delContinent(@RequestBody BaseDelByIdSO delByIdSO){
        return regionService.delContinent(delByIdSO);
    }


    /**
     * 分页查询大洲列表
     * @param queryContinentListSO
     * @return
     */
    @PostMapping("/query/continent/list")
    public ResultMessage queryContinentList(@RequestBody QueryContinentListSO queryContinentListSO){
        return regionService.queryContinentList(queryContinentListSO);
    }



    /**
     * 添加区域国家信息
     * @param addCountrySO
     * @return
     */
    @PostMapping("/add/country")
    public ResultMessage addCountry(@RequestBody AddCountrySO addCountrySO){
        return regionService.addCountry(addCountrySO);
    }


    /**
     * 更新区域国家信息
     * @param updateCountrySO
     * @return
     */
    @PostMapping("/update/country")
    public ResultMessage updateCountry(@RequestBody UpdateCountrySO updateCountrySO){
        return regionService.updateCountry(updateCountrySO);
    }


    /**
     * 删除区域国家信息
     * @param delByIdSO
     * @return
     */
    @PostMapping("/del/country")
    public ResultMessage delCountry(@RequestBody BaseDelByIdSO delByIdSO){
        return regionService.delCountry(delByIdSO);
    }


    /**
     * 分页查询国家列表
     * @param queryCountryListSO
     * @return
     */
    @PostMapping("/query/country/list")
    public ResultMessage queryCountryList(@RequestBody QueryCountryListSO queryCountryListSO){
        return regionService.queryCountryList(queryCountryListSO);
    }

    /**
     * 添加省份信息
     * @param addProvinceSO
     * @return
     */
    @PostMapping("/add/province")
    public ResultMessage addProvince(@RequestBody AddProvinceSO addProvinceSO){
        return regionService.addProvince(addProvinceSO);
    }


    /**
     * 更新省份信息
     * @param updateProvinceSO
     * @return
     */
    @PostMapping("/update/province")
    public ResultMessage updateProvince(@RequestBody UpdateProvinceSO updateProvinceSO){
        return regionService.updateProvince(updateProvinceSO);
    }


    /**
     * 删除省份信息
     * @param delByIdSO
     * @return
     */
    @PostMapping("/del/province")
    public ResultMessage delProvince(@RequestBody BaseDelByIdSO delByIdSO){
        return regionService.delProvince(delByIdSO);
    }


    /**
     * 分页查询省份列表
     * @param queryProvinceListSO
     * @return
     */
    @PostMapping("/query/province/list")
    public ResultMessage queryProvinceList(@RequestBody QueryProvinceListSO queryProvinceListSO){
        return regionService.queryProvinceList(queryProvinceListSO);
    }


    /**
     * 添加城市SO
     * @param addCitySO
     * @return
     */
    @PostMapping("/add/city")
    public ResultMessage addCity(@RequestBody AddCitySO addCitySO){
        return regionService.addCity(addCitySO);
    }

    /**
     * 更新城市信息
     * @param updateCitySO
     * @return
     */
    @PostMapping("/update/city")
    public ResultMessage updateCity(@RequestBody UpdateCitySO updateCitySO){
        return regionService.updateCity(updateCitySO);
    }


    /**
     * 删除城市信息
     * @param delByIdSO
     * @return
     */
    @PostMapping("/del/city")
    public ResultMessage delCity(@RequestBody BaseDelByIdSO delByIdSO){
        return regionService.delCity(delByIdSO);
    }


    /**
     * 分页查询城市信息
     * @param queryCityListSO
     * @return
     */
    @PostMapping("/query/city/list")
    public ResultMessage queryCityList(@RequestBody QueryCityListSO queryCityListSO){
        return regionService.queryCityList(queryCityListSO);
    }


    /**
     * 查询地域下拉选择信息
     * @param queryRegionSelectSO
     * @return
     */
    @PostMapping("/query/region/select")
    public ResultMessage queryRegionSelect(@RequestBody QueryRegionSelectSO queryRegionSelectSO){
        return regionService.queryRegionSelect(queryRegionSelectSO);
    }


    /**
     * 查询产品特性协议列表
     * @param queryAttributeListSO
     * @return
     */
    @PostMapping("/query/attribute/list")
    public ResultMessage queryAttributeList(@RequestBody QueryAttributeListSO queryAttributeListSO){
        return regionService.queryAttributeList(queryAttributeListSO);
    }


    /**
     * 添加产品特性协议
     * @param addAttributeSO
     * @return
     */
    @PostMapping("/add/attribute")
    public ResultMessage addAttribute(@RequestBody AddAttributeSO addAttributeSO){
        return regionService.addAttribute(addAttributeSO);
    }


    /**
     * 更新产品特性协议
     * @param updateAttributeSO
     * @return
     */
    @PostMapping("/update/attribute")
    public ResultMessage updateAttribute(@RequestBody UpdateAttributeSO updateAttributeSO){
        return regionService.updateAttribute(updateAttributeSO);
    }


    /**
     * 删除产品特性协议
     * @param delAttributeSO
     * @return
     */
    @PostMapping("/del/attribute")
    public ResultMessage delAttribute(@RequestBody DelAttributeSO delAttributeSO){
        return regionService.delAttribute(delAttributeSO);
    }


    /**
     * 查询特性协议绑定信息
     * @param queryAttributeBindingSO
     * @return
     */
    @PostMapping("/query/attribute/binding")
    public ResultMessage queryAttributeBinding(@RequestBody QueryAttributeBindingSO queryAttributeBindingSO){
        return regionService.queryAttributeBinding(queryAttributeBindingSO);
    }


    /**
     * 绑定特性协议
     * @param bindingAttributeSO
     * @return
     */
    @PostMapping("/binding/attribute")
    public ResultMessage bindingAttribute(@RequestBody BindingAttributeSO bindingAttributeSO){
        return regionService.bindingAttribute(bindingAttributeSO);
    }


}
