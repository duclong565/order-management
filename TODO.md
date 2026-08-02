# TODO - Feedback mentor (meeting tuan 1)

## Uu tien cao - mentor yeu cau sua

- [x] **Gop `GET /cart/summary` + `PATCH /cart/discount` thanh MOT API**
  - Bo han `PATCH /cart/discount` va `DELETE /cart/discount`
  - Nhan `discountId` lam tham so (null = chua chon ma)
  - Response CHI 4 truong: `subtotal`, `discountAmount`, `shippingFee`, `totalAmount`
  - Bo `cartId`, `items`, `discountId`, `discountName` khoi response
  - He qua: `carts.discount_id` khong con dung

- [x] **`BaseResponse` - format thong nhat cho MOI response (ca thanh cong lan loi)**
  - `{ code, data, message, metadata }`
  - `code` = ma loi unique (khong phai HTTP status), vd 400 -> 4001, 4002...
  - `data` = du lieu thanh cong (object / list / bat ky)
  - `metadata` = thong tin phan trang

- [x] **`ApplicationException` thay `BusinessException`**
  - 2 truong: `code` (ma loi unique) + `status` (HTTP status)
  - Tang service tu quyet HTTP status
  - Tao 1 class tap trung khai bao TOAN BO ma loi he thong (vd `ErrorCode`)

- [x] **Them trang thai ton kho vao response gio hang**
  - `in stock` / `limited stock` / `out of stock` (co trong Figma, dang miss)
  - Tinh o BACKEND: so `cart_item.quantity` voi `inventory.quantity`

- [x] **Sua N+1 trong `GET /cart/me`**
  - Hien tai 4 query: user -> cart -> product_variant -> inventory
  - Dung JOIN FETCH gop lai
  - Nguyen tac: giam round-trip toi da. Tuyet doi khong query trong vong for

- [x] **Bo `total` khoi response gio hang** (Shopee khong tinh tien truoc khi user tick chon)

- [x] **`updateItemQuantity` + `removeItem` khong tra ve ca gio hang** (FE khong dung)

## Convention / cau truc

- [ ] Chuyen `DataSeeder` ra khoi code chinh -> test module
  - CAN QUYET DINH: chuyen han sang `src/test/` thi Run app se KHONG con data demo
    (test source khong nam trong classpath luc chay app)
  - Phuong an thay the: giu o `main` nhung gan `@Profile("dev")` -> production chay
    profile `prod` thi bean nay khong duoc tao. Giai quyet dung moi lo cua mentor
    (khong ro ri data demo ra prod) ma van giu duoc data de demo/test tay
  - Kem theo: doi `System.out.println` -> `log.info` (@Slf4j)
- [ ] DTO: doi `record` -> `class` voi `private` field + getter/setter
- [x] Chuyen enum ra package rieng (`common/`), khong de trong `entity/`
- [ ] Lay user trong tang SERVICE bang `SecurityContextHolder`, khong truyen `userId` tu controller xuong

## Chung

- [ ] BAT BUOC bam Figma - moi field trong response phai co cho dung tren UI
- [ ] Push code xong bao mentor
- [ ] Lam tiep cac API con lai theo doc nghiep vu

---

## No ky thuat (tu phat hien, chua bao mentor)

- [~] Test tu dong: da co PricingCalculatorTest (11 test). Con thieu @DataJpaTest cho repository va @SpringBootTest cho placeOrder
- [ ] Cot `deleted` chua duoc loc o query nao -> soft delete dang vo nghia (`@SQLRestriction`)
- [x] `InventoryRepository.findByProductVariantId` tra `Optional` -> da thay bang `totalStock()` dung `sum()`
- [ ] `CustomUserDetails` doi sang record da mat `isEnabled() = !deleted` -> user xoa mem van login duoc
- [ ] Chua co `POST /cart/items` (them hang vao gio) va `GET /products`
- [ ] 403 chua di qua `GlobalExceptionHandler` (can `AccessDeniedHandler`)
- [ ] Tracking / state machine dang lam do (Phase C-E)
- [x] File rac `DataSeederTest.java` -> da xoa

---

## Phat sinh tu Figma (chua bao mentor)

- [ ] Banner "Validation Error" khi gio co hang het -> can co muc gio
      (vd `hasOutOfStockItems`) hoac FE tu suy tu danh sach items
- [ ] Nut "Re-validate Inventory" -> chinh la goi lai `GET /cart/me`, khong can API moi

---

## Ghi chu ky thuat tu meeting

**`SecurityContextHolder` (chu S hoa - class dung chung) sao lay duoc data rieng tung request?**
Spring chay tren Tomcat theo co che **1 request = 1 thread** (mac dinh 200 thread).
Moi thread co vung nho rieng (`ThreadLocal`) -> class tu nhan biet thread nao dang goi.
Gui 1000 request -> 200 xu ly, 800 xep hang cho thread duoc nha.

**Cung co che do dung cho `traceId` / `requestId` trong log** - nhieu request dan xen
nhung moi dong log mang traceId rieng -> trace duoc tron 1 luong.

**Vi sao nhieu round-trip nghiem trong** (an du cua mentor):
Cho 100 bao cat VN -> My. 1 chuyen cho het vs 100 chuyen moi bao 1 chuyen.
Khong chi la DB stress - ma la chi phi mang: bat tay 3 buoc, latency, ha tang khong on dinh.
Sau nay len microservice thi goi service -> service cung la round-trip.
