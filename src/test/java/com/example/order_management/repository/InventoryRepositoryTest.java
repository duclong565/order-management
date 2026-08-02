package com.example.order_management.repository;

import com.example.order_management.TestcontainersConfiguration;
import com.example.order_management.config.AuditorAwareImpl;
import com.example.order_management.entity.Inventory;
import com.example.order_management.entity.Product;
import com.example.order_management.entity.ProductVariant;
import com.example.order_management.entity.Warehouse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.AuditorAware;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Slice test - chi nap Entity + Repository + DataSource.
 * KHONG nap Controller / Service / Security / bean tu viet.
 *
 * replace = NONE : khong cho Spring thay Postgres bang H2.
 *   H2 cu xu khac Postgres (coalesce, numeric, gen_random_uuid) -> test xanh ma prod no.
 * @Import(TestcontainersConfiguration) : dung Postgres THAT trong Docker.
 *
 * Moi @Test chay trong 1 transaction roi ROLLBACK -> test doc lap, khong can don tay.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, InventoryRepositoryTest.AuditingTestConfig.class})
class InventoryRepositoryTest {

    /**
     * @EnableJpaAuditing tren main class doi bean ten "auditorAwareImpl", nhung
     * @DataJpaTest khong nap @Component. @Import(AuditorAwareImpl.class) cung khong
     * duoc vi no dat ten bean bang FQN. -> khai @Bean, ten METHOD chinh la ten bean.
     */
    @TestConfiguration
    static class AuditingTestConfig {
        @Bean
        AuditorAware<String> auditorAwareImpl() {
            return new AuditorAwareImpl();
        }
    }

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private TestEntityManager em;

    private ProductVariant variant;
    private Warehouse warehouseA;
    private Warehouse warehouseB;

    @BeforeEach
    void setUp() {
        variant = createVariant("Ao thun - Size S", "199000");
        warehouseA = createWarehouse("Kho HCM");
        warehouseB = createWarehouse("Kho Ha Noi");
    }

    // ---------- helper dung du lieu ----------

    private ProductVariant createVariant(String name, String price) {
        Product product = new Product();
        product.setName("Ao thun");
        em.persist(product);                 // cha truoc

        ProductVariant v = new ProductVariant();
        v.setProduct(product);               // gan OBJECT da persist
        v.setName(name);
        v.setPrice(new BigDecimal(price));
        em.persist(v);                       // con sau
        return v;
    }

    private Warehouse createWarehouse(String name) {
        Warehouse w = new Warehouse();
        w.setName(name);
        em.persist(w);
        return w;
    }

    private void createInventory(ProductVariant v, Warehouse w, int quantity) {
        Inventory inv = new Inventory();
        inv.setProductVariant(v);
        inv.setWarehouse(w);
        inv.setQuantity(quantity);
        em.persist(inv);
    }

    /** Day SQL dang cho xuong DB va xoa persistence context. */
    private void flushAndClear() {
        em.flush();   // chay INSERT that su
        em.clear();   // xoa cache -> query sau doc tu DB, khong lay object cu
    }

    // ---------- totalStock ----------

    @Test
    @DisplayName("totalStock: 1 kho ton 100 -> 100")
    void totalStock_withSingleWarehouse_shouldReturnThatQuantity() {
        createInventory(variant, warehouseA, 100);
        flushAndClear();

        long total = inventoryRepository.totalStock(variant.getId());

        assertThat(total).isEqualTo(100);
    }

    @Test
    @DisplayName("totalStock: 2 kho 60 + 40 -> 100 (cong don moi kho)")
    void totalStock_withMultipleWarehouses_shouldSumAllQuantities() {
        createInventory(variant, warehouseA, 60);
        createInventory(variant, warehouseB, 40);
        flushAndClear();

        long total = inventoryRepository.totalStock(variant.getId());

        // Query cu (findByProductVariantId tra Optional) se NO
        // IncorrectResultSizeDataAccessException o dung tinh huong nay.
        assertThat(total).isEqualTo(100);
    }

    @Test
    @DisplayName("totalStock: variant chua nhap kho -> 0, khong phai null")
    void totalStock_whenNoInventoryRow_shouldReturnZero() {
        flushAndClear();   // khong tao inventory nao

        long total = inventoryRepository.totalStock(variant.getId());

        // coalesce(sum(...), 0L) lo phan nay - khong co coalesce thi tra null -> NPE luc unbox
        assertThat(total).isZero();
    }

    @Test
    @DisplayName("totalStock: khong tinh lan ton kho cua variant khac")
    void totalStock_shouldNotCountOtherVariants() {
        ProductVariant otherVariant = createVariant("Ao thun - Size M", "209000");
        createInventory(variant, warehouseA, 100);
        createInventory(otherVariant, warehouseA, 999);
        flushAndClear();

        long total = inventoryRepository.totalStock(variant.getId());

        assertThat(total).isEqualTo(100);
    }

    // ---------- decreaseStock ----------

    @Test
    @DisplayName("decreaseStock: kho 100 tru 10 -> tra 1 dong, con 90")
    void decreaseStock_whenEnoughStock_shouldDecreaseAndReturnOne() {
        createInventory(variant, warehouseA, 100);
        flushAndClear();

        int updated = inventoryRepository.decreaseStock(variant.getId(), 10);

        assertThat(updated).isEqualTo(1);

        em.clear();   // @Modifying chay SQL thang, khong qua context -> phai clear moi doc duoc so moi
        assertThat(inventoryRepository.totalStock(variant.getId())).isEqualTo(90);
    }

    @Test
    @DisplayName("decreaseStock: kho 5 tru 10 -> tra 0 dong, kho VAN 5")
    void decreaseStock_whenNotEnoughStock_shouldReturnZeroAndNotChangeStock() {
        createInventory(variant, warehouseA, 5);
        flushAndClear();

        int updated = inventoryRepository.decreaseStock(variant.getId(), 10);

        // Dieu kien `and i.quantity >= :qty` chan lai -> 0 dong bi sua.
        // Day la tin hieu "het hang" ma service phai kiem: if (updated == 0) throw.
        // Bug cu viet `updated < 0` -> khong bao gio dung -> oversell.
        assertThat(updated).isZero();

        em.clear();
        assertThat(inventoryRepository.totalStock(variant.getId())).isEqualTo(5);
    }

    @Test
    @DisplayName("decreaseStock: kho 10 tru dung 10 -> tra 1 dong, con 0 (bien >=)")
    void decreaseStock_whenStockEqualsRequested_shouldSucceed() {
        createInventory(variant, warehouseA, 10);
        flushAndClear();

        int updated = inventoryRepository.decreaseStock(variant.getId(), 10);

        assertThat(updated).isEqualTo(1);

        em.clear();
        assertThat(inventoryRepository.totalStock(variant.getId())).isZero();
    }

    @Test
    @DisplayName("decreaseStock: variant khong ton tai -> tra 0 dong")
    void decreaseStock_whenVariantNotFound_shouldReturnZero() {
        flushAndClear();

        int updated = inventoryRepository.decreaseStock(UUID.randomUUID(), 1);

        assertThat(updated).isZero();
    }
}
