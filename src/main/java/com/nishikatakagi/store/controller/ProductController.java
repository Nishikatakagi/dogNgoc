package com.nishikatakagi.store.controller;

import com.nishikatakagi.store.mapper.ProductMapper;
import com.nishikatakagi.store.models.Product;
import com.nishikatakagi.store.models.ProductDto;
import com.nishikatakagi.store.models.ProductHistory;
import com.nishikatakagi.store.repository.ProductHistoryRepository;
import com.nishikatakagi.store.repository.ProductRepository;

import jakarta.validation.Valid;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.List;

@Controller
@RequestMapping("/products")
public class ProductController {
    private ProductRepository pr;
    private ProductHistoryRepository phr;

    public ProductController(ProductRepository pr, ProductHistoryRepository phr) {
        this.pr = pr;
        this.phr = phr;
    }

    @GetMapping({"", "/"})
    public String showProductList(Model model){
        List<Product> products = pr.findAll();
        model.addAttribute("hi","hello");
        model.addAttribute("products",products);
        return "products/index";
    }
    
    @GetMapping("/incre")
    public String showProductListIncrebyPrice(Model model){
        List<Product> products = pr.findAll(Sort.by(Sort.Direction.ASC,"price"));
        model.addAttribute("products",products);
        return "products/incre";
    }
    
    // xóa một sản phẩm theo id và lưu trữ vào bảng producthistory
    @GetMapping("/delete/{id}")
    public String deleteAndShow(Model model,@PathVariable int id){
    	Product p = pr.findById(id).orElseThrow(() -> new RuntimeException("Account does not exist"));
    	ProductHistory ph = ProductMapper.convertt(p);
    	phr.save(ph);
    	pr.deleteById(id);
        List<Product> products = pr.findAll();
        model.addAttribute("products",products);
        return "products/index";
    }
    
    // hiện thị danh sách các sản phẩm đã xóa, lấy db từ bảng productHistory
    @GetMapping("/history")
    public String showProductListHistory(Model model){
        List<ProductHistory> products = phr.findAll();
        model.addAttribute("products",products);
        return "products/history";
    }
    
    @GetMapping("/restore/{id}")
    public String restoreProduct(Model model,@PathVariable int id){
    	ProductHistory p = phr.findById(id).orElseThrow(() -> new RuntimeException("Account does not exist"));
    	Product ph = ProductMapper.convertt(p);
    	pr.save(ph);
    	phr.deleteById(id);
        List<ProductHistory> products = phr.findAll();
        model.addAttribute("products",products);
        return "products/history";
    }
    
    @GetMapping("/create")
    public String showCreatePage(Model model) {
    	ProductDto productDto = new ProductDto();
    	model.addAttribute("productDto",productDto);
    	return "products/CreateProduct";
    }
    
    @PostMapping("/create")
    //@ModelAttribute ràng buộc dữ liệu biểu mẫu được gửi bởi người dùng vào đối tượng này.
    //@Valid được sử dụng để yêu cầu Spring Framework thực hiện kiểm tra hợp lệ cho đối tượng productDto
    
//    BindingResult là một đối tượng được sử dụng để chứa kết quả của quá trình ánh xạ dữ liệu từ yêu cầu 
//    HTTP vào một đối tượng Java, thông qua @ModelAttribute, và thực hiện việc kiểm tra hợp lệ của dữ liệu 
//    nếu được yêu cầu bằng cách sử dụng @Valid.
    //Nếu có bất kỳ lỗi nào được phát hiện trong quá trình kiểm tra hợp lệ, thông tin về các lỗi đó sẽ được lưu trữ trong đối tượng BindingResult
    
//    @Valid:là annotation của Spring Framework được sử dụng để đánh dấu tham số productDto. Khi một đối tượng 
//    được đánh dấu bằng @Valid, Spring sẽ thực hiện việc kiểm tra hợp lệ cho đối tượng đó sau khi nó được ánh xạ từ dữ 
//    liệu yêu cầu HTTP. Điều này có nghĩa là các ràng buộc hợp lệ được định nghĩa trong lớp ProductDto sẽ được áp dụng 
//    và bất kỳ lỗi nào xảy ra sẽ được lưu trữ trong đối tượng BindingResult.
    public String createProduct(
    		@Valid @ModelAttribute ProductDto productDto,
    		BindingResult result) {
    	
    	if(productDto.getImgFile().isEmpty()) {
//    		Đối tượng FieldError được tạo mới. Trong đó:
//    			"productDto": Đây là tên của đối tượng gây ra lỗi, trong trường hợp này là productDto.
//    			"imgFile": Đây là tên của trường hoặc thuộc tính gây ra lỗi, ở đây là imgFile.
//    			"The image file is required": Đây là thông báo lỗi, cho biết rằng tệp hình ảnh là bắt buộc.
    		result.addError(new FieldError("productDto","imgFile","The image file is requied"));
    	}
    	
    	if(result.hasErrors()) {
    		return "products/CreateProduct";
    	}
    	
    	// Save image file
    	MultipartFile image = productDto.getImgFile();
    	Date createAt = new Date();
    	// tạo ra một tên tệp lưu trữ duy nhất cho tệp hình ảnh được tải lên. 
    	// với các thuộc tính : ngày +  _  + tên file gốc người dùng gửi 
    	String storageFileName = createAt.getTime() + "_" + image.getOriginalFilename();
    	
     	try {
     		//Đường dẫn của thư mục nơi tệp hình ảnh sẽ được lưu trữ.
			String uploadDir = "public/images/";
			
			// Tạo một đối tượng Path từ đường dẫn thư mục.
			Path uploadPath = Paths.get(uploadDir);
			
//			Dòng mã này kiểm tra xem thư mục lưu trữ tệp đã tồn tại hay chưa. Nếu thư mục không tồn tại, 
//			nó sẽ tạo ra thư mục đó và tất cả các thư mục cha cần thiết để tạo ra thư mục lưu trữ.
			if(!Files.exists(uploadPath)) {
				Files.createDirectories(uploadPath);
			}
			
			//Dòng mã này thực hiện việc sao chép dữ liệu từ InputStream vào một tệp trong thư mục lưu trữ
			try (InputStream inputStream = image.getInputStream()){
				Files.copy(inputStream, Paths.get(uploadDir + storageFileName),
						StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
     	
     	Product product = new Product();
     	product.setName(productDto.getName());
     	product.setBrand(productDto.getBrand());
     	product.setCategory(productDto.getCategory());
     	product.setPrice(productDto.getPrice());
     	product.setDescription(productDto.getDescription());
     	product.setCreateAt(createAt);
     	product.setImgFileName(storageFileName);
     	
     	pr.save(product);
    	return "redirect:/products";
    }
    		
    		
    @GetMapping("/edit")
    public String showEditPage(Model model, @RequestParam int id) {
    	try {
    		Product product = pr.findById(id).get();
    		model.addAttribute("product", product);
    		
    		ProductDto productDto = new ProductDto();
    		productDto.setName(product.getName());
    		productDto.setBrand(product.getBrand());
    		productDto.setCategory(product.getCategory());
    		productDto.setPrice(product.getPrice());
    		productDto.setDescription(product.getDescription());
    		
    		model.addAttribute("productDto",productDto);
		} catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
			return "redirect:/products";
		}
    	return "products/edit";
    }
    
    
    @PostMapping("/edit")
    public String updateProduct(Model model, @RequestParam int id,
    		@Valid @ModelAttribute ProductDto productDto,
    		BindingResult resule) {
    	try {
			Product product = pr.findById(id).get();
			model.addAttribute("product",product);
			
			if(resule.hasErrors()) {
				return "products/edit";
			}
			
			if(!productDto.getImgFile().isEmpty()) {
				//delete old image
				String uploadDir = "public/images/";
				Path oldImagePath = Paths.get(uploadDir + product.getImgFileName());
				
				try {
					Files.delete(oldImagePath);
				} catch (Exception e) {
					System.out.println("Exception: " + e.getMessage());
				}
				
				// save new image file
				MultipartFile image = productDto.getImgFile();
		    	Date createAt = new Date();
		    	String storageFileName = createAt.getTime() + "_" + image.getOriginalFilename();
		    	try (InputStream inputStream = image.getInputStream()){
					Files.copy(inputStream, Paths.get(uploadDir + storageFileName),
							StandardCopyOption.REPLACE_EXISTING);
				}
		    	
		    	product.setImgFileName(storageFileName);  
			}
			
			product.setName(productDto.getName());
    		product.setBrand(productDto.getBrand());
    		product.setCategory(productDto.getCategory());
    		product.setPrice(productDto.getPrice());
    		product.setDescription(productDto.getDescription());
			
    		pr.save(product);
    		
		} catch (Exception e) {
			System.out.println("Exception: " + e.getMessage());
		}
    	return "redirect:/products";
    }
}
