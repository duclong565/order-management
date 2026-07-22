package com.example.order_management.config;

import com.example.order_management.entity.Address;
import com.example.order_management.entity.Cart;
import com.example.order_management.entity.CartItem;
import com.example.order_management.entity.Discount;
import com.example.order_management.entity.DiscountType;
import com.example.order_management.entity.Inventory;
import com.example.order_management.entity.PaymentMethod;
import com.example.order_management.entity.PaymentMethodType;
import com.example.order_management.entity.Product;
import com.example.order_management.entity.ProductVariant;
import com.example.order_management.entity.User;
import com.example.order_management.entity.UserRole;
import com.example.order_management.entity.Warehouse;
import com.example.order_management.repository.AddressRepository;
import com.example.order_management.repository.CartItemRepository;
import com.example.order_management.repository.CartRepository;
import com.example.order_management.repository.DiscountRepository;
import com.example.order_management.repository.InventoryRepository;
import com.example.order_management.repository.PaymentMethodRepository;
import com.example.order_management.repository.ProductRepository;
import com.example.order_management.repository.ProductVariantRepository;
import com.example.order_management.repository.UserRepository;
import com.example.order_management.repository.WarehouseRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
@AllArgsConstructor
class DataSeeder {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final DiscountRepository discountRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    @Bean
    CommandLineRunner seedData() {
        return args -> {
            seedUsers();
            seedAddresses();
            seedCatalog();
            seedWarehouseAndInventory();
            seedDiscounts();
            seedPaymentMethods();
            seedCart();
        };
    }

    private void seedAddresses() {
        if (addressRepository.count() > 0) {
            return;
        }

        User admin = userRepository.findByUsername("admin").orElseThrow();

        Address address = new Address();
        address.setUser(admin);
        address.setLine1("123 Nguyen Hue");
        address.setLine2("Tang 5");
        address.setCity("Ho Chi Minh");
        address.setState("Ho Chi Minh");
        address.setCountry("Vietnam");
        address.setZipCode("700000");
        addressRepository.save(address);

        System.out.println("Seeded 1 address for admin");
    }

    private void seedPaymentMethods() {
        if (paymentMethodRepository.count() > 0) {
            return;
        }

        PaymentMethod cod = new PaymentMethod();
        cod.setName("COD");
        cod.setDescription("Thanh toan khi nhan hang");
        cod.setType(PaymentMethodType.CASH);
        paymentMethodRepository.save(cod);

        PaymentMethod vnpay = new PaymentMethod();
        vnpay.setName("VNPay");
        vnpay.setDescription("Thanh toan qua cong VNPay");
        vnpay.setType(PaymentMethodType.VNPAY);
        paymentMethodRepository.save(vnpay);

        System.out.println("Seeded 2 payment methods: COD, VNPay");
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin"));
        admin.setEmail("admin@example.com");
        admin.setRole(UserRole.ADMIN);
        userRepository.save(admin);

        System.out.println("Seeded user: admin / admin");
    }

    private void seedCatalog() {
        if (productRepository.count() > 0) {
            return;
        }

        Product aoThun = new Product();
        aoThun.setName("Ao thun");
        aoThun.setDescription("Ao thun cotton");
        Product savedAoThun = productRepository.save(aoThun);

        Product quanJean = new Product();
        quanJean.setName("Quan jean");
        quanJean.setDescription("Quan jean nam");
        Product savedQuanJean = productRepository.save(quanJean);

        saveVariant(savedAoThun, "Ao thun - Size S", "199000");
        saveVariant(savedAoThun, "Ao thun - Size M", "209000");
        saveVariant(savedQuanJean, "Quan jean - Size 30", "499000");
        saveVariant(savedQuanJean, "Quan jean - Size 32", "519000");

        System.out.println("Seeded 2 products / 4 variants");
    }

    private void saveVariant(Product product, String name, String price) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setName(name);
        variant.setPrice(new BigDecimal(price));
        productVariantRepository.save(variant);
    }

    private void seedWarehouseAndInventory() {
        if (warehouseRepository.count() > 0) {
            return;
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setName("Kho HCM");
        warehouse.setDescription("Kho trung tam TP HCM");
        Warehouse savedWarehouse = warehouseRepository.save(warehouse);

        for (ProductVariant variant : productVariantRepository.findAll()) {
            Inventory inventory = new Inventory();
            inventory.setProductVariant(variant);
            inventory.setWarehouse(savedWarehouse);
            inventory.setQuantity(100);
            inventoryRepository.save(inventory);
        }

        System.out.println("Seeded 1 warehouse + inventory (100/variant)");
    }

    private void seedDiscounts() {
        if (discountRepository.count() > 0) {
            return;
        }

        Instant now = Instant.now();

        Discount percent = new Discount();
        percent.setName("SALE10");
        percent.setDescription("Giam 10% tren tong don");
        percent.setType(DiscountType.PERCENT);
        percent.setValue(new BigDecimal("10"));
        percent.setStartDate(now);
        percent.setEndDate(now.plus(30, ChronoUnit.DAYS));
        discountRepository.save(percent);

        Discount fixed = new Discount();
        fixed.setName("GIAM50K");
        fixed.setDescription("Giam 50000 tren tong don");
        fixed.setType(DiscountType.FIXED);
        fixed.setValue(new BigDecimal("50000"));
        fixed.setStartDate(now);
        fixed.setEndDate(now.plus(30, ChronoUnit.DAYS));
        discountRepository.save(fixed);

        System.out.println("Seeded 2 discounts: SALE10 (PERCENT), GIAM50K (FIXED)");
    }

    private void seedCart() {
        if (cartRepository.count() > 0) {
            return;
        }

        User admin = userRepository.findByUsername("admin").orElseThrow();

        Cart cart = new Cart();
        cart.setUser(admin);
        Cart savedCart = cartRepository.save(cart);

        List<ProductVariant> variants = productVariantRepository.findAll();
        saveCartItem(savedCart, variants.get(0), 2);
        saveCartItem(savedCart, variants.get(1), 1);

        System.out.println("Seeded cart for admin with 2 items");
    }

    private void saveCartItem(Cart cart, ProductVariant variant, int quantity) {
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProductVariant(variant);
        item.setQuantity(quantity);
        cartItemRepository.save(item);
    }
}
