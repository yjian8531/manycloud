package com.core.manycloudadmin.service;

import com.core.manycloudadmin.so.BaseDelByIdSO;
import com.core.manycloudadmin.so.admin.*;
import com.core.manycloudadmin.so.region.*;
import com.core.manycloudcommon.utils.ResultMessage;
import org.springframework.transaction.annotation.Transactional;

public interface RegionService {

    /**
     * 添加大洲信息
     * @param addContinentSO
     * @return
     */
    ResultMessage addContinent(AddContinentSO addContinentSO);


    /**
     * 更新大洲信息
     * @param updateContinentSO
     * @return
     */
    ResultMessage updateContinent(UpdateContinentSO updateContinentSO);


    /**
     * 删除大洲信息
     * @param delByIdSO
     * @return
     */
    ResultMessage delContinent(BaseDelByIdSO delByIdSO);


    /**
     * 分页查询大洲列表
     * @param queryContinentListSO
     * @return
     */
    ResultMessage queryContinentList(QueryContinentListSO queryContinentListSO);



    /**
     * 添加区域国家信息
     * @param addCountrySO
     * @return
     */
    ResultMessage addCountry(AddCountrySO addCountrySO);


    /**
     * 更新区域国家信息
     * @param updateCountrySO
     * @return
     */
    ResultMessage updateCountry(UpdateCountrySO updateCountrySO);


    /**
     * 删除区域国家信息
     * @param delByIdSO
     * @return
     */
    ResultMessage delCountry(BaseDelByIdSO delByIdSO);


    /**
     * 分页查询国家列表
     * @param queryCountryListSO
     * @return
     */
    ResultMessage queryCountryList(QueryCountryListSO queryCountryListSO);

    /**
     * 添加省份信息
     * @param addProvinceSO
     * @return
     */
    ResultMessage addProvince(AddProvinceSO addProvinceSO);


    /**
     * 更新省份信息
     * @param updateProvinceSO
     * @return
     */
    ResultMessage updateProvince(UpdateProvinceSO updateProvinceSO);


    /**
     * 删除省份信息
     * @param delByIdSO
     * @return
     */
    ResultMessage delProvince(BaseDelByIdSO delByIdSO);


    /**
     * 分页查询省份列表
     * @param queryProvinceListSO
     * @return
     */
    ResultMessage queryProvinceList(QueryProvinceListSO queryProvinceListSO);


    /**
     * 添加城市SO
     * @param addCitySO
     * @return
     */
    ResultMessage addCity(AddCitySO addCitySO);

    /**
     * 更新城市信息
     * @param updateCitySO
     * @return
     */
    ResultMessage updateCity(UpdateCitySO updateCitySO);


    /**
     * 删除城市信息
     * @param delByIdSO
     * @return
     */
    ResultMessage delCity(BaseDelByIdSO delByIdSO);


    /**
     * 分页查询城市信息
     * @param queryCityListSO
     * @return
     */
    ResultMessage queryCityList(QueryCityListSO queryCityListSO);


    /**
     * 查询地域下拉选择信息
     * @param queryRegionSelectSO
     * @return
     */
    ResultMessage queryRegionSelect(QueryRegionSelectSO queryRegionSelectSO);


    /**
     * 查询产品特性协议列表
     * @param queryAttributeListSO
     * @return
     */
    ResultMessage queryAttributeList(QueryAttributeListSO queryAttributeListSO);


    /**
     * 添加产品特性协议
     * @param addAttributeSO
     * @return
     */
    ResultMessage addAttribute(AddAttributeSO addAttributeSO);


    /**
     * 更新产品特性协议
     * @param updateAttributeSO
     * @return
     */
    ResultMessage updateAttribute(UpdateAttributeSO updateAttributeSO);


    /**
     * 删除产品特性协议
     * @param delAttributeSO
     * @return
     */
    ResultMessage delAttribute(DelAttributeSO delAttributeSO);


    /**
     * 查询特性协议绑定信息
     * @param queryAttributeBindingSO
     * @return
     */
    ResultMessage queryAttributeBinding(QueryAttributeBindingSO queryAttributeBindingSO);


    /**
     * 绑定特性协议
     * @param bindingAttributeSO
     * @return
     */
    ResultMessage bindingAttribute(BindingAttributeSO bindingAttributeSO);

}
