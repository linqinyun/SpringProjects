package com.imooc.o2o.web.shopadmin;

import java.io.IOException;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.imooc.o2o.dto.EchartSeries;
import com.imooc.o2o.dto.EchartXAxis;
import com.imooc.o2o.dto.ShopAuthMapExecution;
import com.imooc.o2o.dto.UserAccessToken;
import com.imooc.o2o.dto.UserProductMapExecution;
import com.imooc.o2o.entity.PersonInfo;
import com.imooc.o2o.entity.Product;
import com.imooc.o2o.entity.ProductSellDaily;
import com.imooc.o2o.entity.Shop;
import com.imooc.o2o.entity.ShopAuthMap;
import com.imooc.o2o.entity.UserProductMap;
import com.imooc.o2o.enums.UserProductMapStateEnum;
import com.imooc.o2o.service.ProductSellDailyService;
import com.imooc.o2o.service.ProductService;
import com.imooc.o2o.service.ShopAuthMapService;
import com.imooc.o2o.service.UserProductMapService;
import com.imooc.o2o.util.HttpServletRequestUtil;

@Controller
@RequestMapping("/shopadmin")
public class UserProductManagementController {
	@Autowired
	private UserProductMapService userProductMapService;
	@Autowired
	private ProductSellDailyService productSellDailyService;
	@Autowired
	private ShopAuthMapService shopAuthMapService;
	@Autowired
	private ProductService productService;

	@RequestMapping(value = "/listuserproductmapsbyshop", method = RequestMethod.GET)
	@ResponseBody
	private Map<String, Object> listUserProductMapsByShop(HttpServletRequest request) {
		Map<String, Object> modelMap = new HashMap<String, Object>();
		// 获取分页信息
		int pageIndex = HttpServletRequestUtil.getInt(request, "pageIndex");
		int pageSize = HttpServletRequestUtil.getInt(request, "pageSize");
		// 获取当前的店铺信息
		Shop currentShop = (Shop) request.getSession().getAttribute("currentShop");
		// 空值校验，主要确保shopId不为空
		if ((pageIndex > -1) && (pageSize > -1) && (currentShop != null) && (currentShop.getShopId() != null)) {
			// 添加查询条件
			UserProductMap userProductMapCondition = new UserProductMap();
			userProductMapCondition.setShop(currentShop);
			String productName = HttpServletRequestUtil.getString(request, "productName");
			if (productName != null) {
				// 若前端想按照商品名模糊查询，则传入productName
				Product product = new Product();
				product.setProductName(productName);
				userProductMapCondition.setProduct(product);
			}
			// 根据传入的查询条件获取该店铺的商品销售情况
			UserProductMapExecution ue = userProductMapService.listUserProductMap(userProductMapCondition, pageIndex,
					pageSize);
			modelMap.put("userProductMapList", ue.getUserProductMapList());
			modelMap.put("count", ue.getCount());
			modelMap.put("success", true);
		} else {
			modelMap.put("success", false);
			modelMap.put("errMsg", "empty pageSize or pageIndex or shopId");
		}
		return modelMap;
	}

	@RequestMapping(value = "/listproductselldailyinfobyshop", method = RequestMethod.GET)
	@ResponseBody
	private Map<String, Object> listProductSellDailyInfobyShop(HttpServletRequest request) throws ParseException {
		Map<String, Object> modelMap = new HashMap<String, Object>();
		// 获取当前的店铺信息
		Shop currentShop = (Shop) request.getSession().getAttribute("currentShop");
		// 空值校验，主要确保shopId不为空
		if ((currentShop != null) && (currentShop.getShopId() != null)) {
			// 添加查询条件
			ProductSellDaily productSellDailyCondition = new ProductSellDaily();
			productSellDailyCondition.setShop(currentShop);
			Calendar calendar = Calendar.getInstance();
			// 获取昨天的日期
			calendar.add(Calendar.DATE, -1);
			Date endTime = calendar.getTime();
			// 获取七天前的日期
			calendar.add(Calendar.DATE, -6);
			Date beginTime = calendar.getTime();
			// 根据传入的查询条件获取该店铺的商品销售情况
			List<ProductSellDaily> productSellDailyList = productSellDailyService
					.listProductSellDaily(productSellDailyCondition, beginTime, endTime);
			// 指定日期格式
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
			// 商品名列表，保证唯一性
			LinkedHashSet<String> legendData = new LinkedHashSet<String>();
			// x轴数据
			LinkedHashSet<String> xData = new LinkedHashSet<String>();
			// 定义series
			List<EchartSeries> series = new ArrayList<EchartSeries>();
			// 日销量列表
			List<Integer> totalList = new ArrayList<Integer>();
			// 当前商品名，默认为空
			String currentProductName = "";
			for (int i = 0; i < productSellDailyList.size(); i++) {
				ProductSellDaily productSellDaily = productSellDailyList.get(i);
				// 自动去重
				legendData.add(productSellDaily.getProduct().getProductName());
				xData.add(sdf.format(productSellDaily.getCreateTime()));
				if (!currentProductName.equals(productSellDaily.getProduct().getProductName())
						&& !currentProductName.isEmpty()) {
					// 如果currentProductName不等于获取的商品名，或者已遍历到列表的末尾，且currentProductName不为空，
					// 则是遍历到下一个商品的日销量信息了, 将前一轮遍历的信息放入series当中，
					// 包括了商品名以及与商品对应的统计日期以及当日销量
					EchartSeries es = new EchartSeries();
					es.setName(currentProductName);
					es.setData(totalList.subList(0, totalList.size()));
					series.add(es);
					// 重置totalList
					totalList = new ArrayList<Integer>();
					// 变换下currentProductId为当前的productId
					currentProductName = productSellDaily.getProduct().getProductName();
					// 继续添加新的值
					totalList.add(productSellDaily.getTotal());
				} else {
					// 如果还是当前的productId则继续添加新值
					totalList.add(productSellDaily.getTotal());
					currentProductName = productSellDaily.getProduct().getProductName();
				}
				// 队列之末，需要将最后的一个商品销量信息也添加上
				if (i == productSellDailyList.size() - 1) {
					EchartSeries es = new EchartSeries();
					es.setName(currentProductName);
					es.setData(totalList.subList(0, totalList.size()));
					series.add(es);
				}
			}
			modelMap.put("series", series);
			modelMap.put("legendData", legendData);
			// 拼接出xAxis
			List<EchartXAxis> xAxis = new ArrayList<EchartXAxis>();
			EchartXAxis exa = new EchartXAxis();
			exa.setData(xData);
			xAxis.add(exa);
			modelMap.put("xAxis", xAxis);
			modelMap.put("success", true);
		} else {
			modelMap.put("success", false);
			modelMap.put("errMsg", "empty shopId");
		}
		return modelMap;
	}



	/**
	 * 根据传入的customerId, productId以及操作员信息组建用户消费记录
	 * 
	 * @param customerId
	 * @param productId
	 * @param operator
	 * @return
	 */
	private UserProductMap compactUserProductMap4Add(Long customerId, Long productId, PersonInfo operator) {
		UserProductMap userProductMap = null;
		if (customerId != null && productId != null) {
			userProductMap = new UserProductMap();
			PersonInfo customer = new PersonInfo();
			customer.setUserId(customerId);
			// 主要为了获取商品积分
			Product product = productService.getProductById(productId);
			userProductMap.setProduct(product);
			userProductMap.setShop(product.getShop());
			userProductMap.setUser(customer);
			userProductMap.setPoint(product.getPoint());
			userProductMap.setCreateTime(new Date());
			userProductMap.setOperator(operator);
		}
		return userProductMap;
	}

	/**
	 * 检查扫码的人员是否有操作权限
	 * 
	 * @param userId
	 * @param userProductMap
	 * @return
	 */
	private boolean checkShopAuth(long userId, UserProductMap userProductMap) {
		// 获取该店铺的所有授权信息
		ShopAuthMapExecution shopAuthMapExecution = shopAuthMapService
				.listShopAuthMapByShopId(userProductMap.getShop().getShopId(), 1, 1000);
		for (ShopAuthMap shopAuthMap : shopAuthMapExecution.getShopAuthMapList()) {
			// 看看是否给过该人员进行授权
			if (shopAuthMap.getEmployee().getUserId() == userId) {
				return true;
			}
		}
		return false;
	}

}
