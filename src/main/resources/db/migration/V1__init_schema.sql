-- 1. users
create table users (
    id          uuid         primary key default gen_random_uuid(),
    username    varchar(50)  not null unique,
    email       varchar(255) not null unique,
    password    varchar(255) not null,
    role        varchar(20)  not null
        constraint chk_users_role check (role in ('ADMIN','CUSTOMER','SELLER','DELIVERY_PERSON')),

    created_at  timestamptz  not null default now(),
    created_by  varchar(50),
    updated_at  timestamptz,
    updated_by  varchar(50),
    deleted     boolean      not null default false
);

-- 2. addresses -> users
create table addresses (
    id          uuid         primary key default gen_random_uuid(),
    user_id     uuid         not null,
    line1       varchar(255),
    line2       varchar(255),
    city        varchar(100) not null,
    state       varchar(100) not null,
    country     varchar(100) not null,
    zip_code    varchar(20),

    created_at  timestamptz  not null default now(),
    created_by  varchar(50),
    updated_at  timestamptz,
    updated_by  varchar(50),
    deleted     boolean      not null default false,

    constraint fk_addresses_user foreign key (user_id) references users(id)
);

-- 3. warehouses -> addresses
create table warehouses (
    id          uuid         primary key default gen_random_uuid(),
    name        varchar(100) not null,
    description text,
    address_id  uuid,

    created_at  timestamptz  not null default now(),
    created_by  varchar(50),
    updated_at  timestamptz,
    updated_by  varchar(50),
    deleted     boolean      not null default false,

    constraint fk_warehouses_address foreign key (address_id) references addresses(id)
);

-- 4. payment_methods
create table payment_methods (
    id          uuid         primary key default gen_random_uuid(),
    name        varchar(50)  not null unique,
    description text,
    type        varchar(20)  not null
        constraint chk_payment_methods_type check (type in ('VNPAY','MOMO','STRIPE','CASH')),

    created_at  timestamptz  not null default now(),
    created_by  varchar(50),
    updated_at  timestamptz,
    updated_by  varchar(50),
    deleted     boolean      not null default false
);

-- 5. discounts
create table discounts (
    id          uuid         primary key default gen_random_uuid(),
    name        varchar(100) not null,
    description text,
    value       numeric(12,2) not null check (value >= 0),
    type        varchar(20)  not null
        constraint chk_discounts_type check (type in ('PERCENT','FIXED')),
    start_date  timestamptz,
    end_date    timestamptz,

    created_at  timestamptz  not null default now(),
    created_by  varchar(50),
    updated_at  timestamptz,
    updated_by  varchar(50),
    deleted     boolean      not null default false,

    constraint chk_discounts_dates check (end_date is null or start_date is null or end_date > start_date)
);

-- 6. products
create table products (
    id          uuid         primary key default gen_random_uuid(),
    name        varchar(255) not null,
    description text,

    created_at  timestamptz  not null default now(),
    created_by  varchar(50),
    updated_at  timestamptz,
    updated_by  varchar(50),
    deleted     boolean      not null default false
);

-- 7. product_variants -> products
create table product_variants (
    id          uuid         primary key default gen_random_uuid(),
    product_id  uuid         not null,
    name        varchar(255) not null,
    price       numeric(12,2) not null check (price >= 0),

    created_at  timestamptz  not null default now(),
    created_by  varchar(50),
    updated_at  timestamptz,
    updated_by  varchar(50),
    deleted     boolean      not null default false,

    constraint fk_product_variants_product foreign key (product_id) references products(id)
);

-- 8. inventories -> product_variants, warehouses  (ton kho theo tung kho)
create table inventories (
    id                 uuid  primary key default gen_random_uuid(),
    product_variant_id uuid  not null,
    warehouse_id       uuid  not null,
    quantity           int   not null default 0 check (quantity >= 0),

    created_at  timestamptz  not null default now(),
    created_by  varchar(50),
    updated_at  timestamptz,
    updated_by  varchar(50),
    deleted     boolean      not null default false,

    constraint fk_inventories_variant   foreign key (product_variant_id) references product_variants(id),
    constraint fk_inventories_warehouse foreign key (warehouse_id) references warehouses(id),
    constraint uq_inventories_variant_warehouse unique (product_variant_id, warehouse_id)
);

-- 9. carriers -> addresses
create table carriers (
    id          uuid         primary key default gen_random_uuid(),
    name        varchar(100) not null,
    description text,
    type        varchar(20)  not null
        constraint chk_carriers_type check (type in ('GIAOHANGNHANH','VIETTELPOST')),
    address_id  uuid,

    created_at  timestamptz  not null default now(),
    created_by  varchar(50),
    updated_at  timestamptz,
    updated_by  varchar(50),
    deleted     boolean      not null default false,

    constraint fk_carriers_address foreign key (address_id) references addresses(id)
);

-- 10. carts -> users, discounts
create table carts (
    id          uuid         primary key default gen_random_uuid(),
    user_id     uuid         not null,
    discount_id uuid,

    created_at  timestamptz  not null default now(),
    created_by  varchar(50),
    updated_at  timestamptz,
    updated_by  varchar(50),
    deleted     boolean      not null default false,

    constraint fk_carts_user     foreign key (user_id) references users(id),
    constraint fk_carts_discount foreign key (discount_id) references discounts(id)
);

-- 11. cart_items -> carts, product_variants
create table cart_items (
    id                 uuid  primary key default gen_random_uuid(),
    cart_id            uuid  not null,
    product_variant_id uuid  not null,
    quantity           int   not null check (quantity > 0),

    created_at  timestamptz  not null default now(),
    created_by  varchar(50),
    updated_at  timestamptz,
    updated_by  varchar(50),
    deleted     boolean      not null default false,

    constraint fk_cart_items_cart    foreign key (cart_id) references carts(id),
    constraint fk_cart_items_variant foreign key (product_variant_id) references product_variants(id),
    constraint uq_cart_items_cart_variant unique (cart_id, product_variant_id)
);

-- 12. orders -> users, discounts, payment_methods, carriers, addresses
create table orders (
    id                      uuid          primary key default gen_random_uuid(),
    user_id                 uuid          not null,
    status                  varchar(20)   not null default 'PENDING'
        constraint chk_orders_status
            check (status in ('PENDING','CONFIRMED','PICKING','SHIPPING','DELIVERED','CANCELLED','FAILED','RETURNING','REATTEMPT')),
    discount_id             uuid,
    discount_value          numeric(12,2) check (discount_value >= 0),
    subtotal_price          numeric(12,2) not null check (subtotal_price >= 0),
    shipping_fee            numeric(12,2) not null default 0 check (shipping_fee >= 0),
    total_price             numeric(12,2) not null check (total_price >= 0),
    payment_method_id       uuid,
    payment_status          varchar(20)   not null default 'UNPAID'
        constraint chk_orders_payment_status check (payment_status in ('UNPAID','AWAITING_PAYMENT','PAID')),
    carrier_id              uuid,
    tracking_number         varchar(50)   unique,
    estimated_delivery_date timestamptz,
    sender_address_id       uuid,
    sender_address          varchar(500),
    recipient_address_id    uuid,
    recipient_address       varchar(500),

    created_at  timestamptz  not null default now(),
    created_by  varchar(50),
    updated_at  timestamptz,
    updated_by  varchar(50),
    deleted     boolean      not null default false,

    constraint fk_orders_user           foreign key (user_id) references users(id),
    constraint fk_orders_discount        foreign key (discount_id) references discounts(id),
    constraint fk_orders_payment_method  foreign key (payment_method_id) references payment_methods(id),
    constraint fk_orders_carrier         foreign key (carrier_id) references carriers(id),
    constraint fk_orders_sender_address    foreign key (sender_address_id) references addresses(id),
    constraint fk_orders_recipient_address foreign key (recipient_address_id) references addresses(id)
);

-- 13. order_items -> orders, product_variants  (snapshot unit_price)
create table order_items (
    id                 uuid          primary key default gen_random_uuid(),
    order_id           uuid          not null,
    product_variant_id uuid          not null,
    quantity           int           not null check (quantity > 0),
    unit_price         numeric(12,2) not null check (unit_price >= 0),

    created_at  timestamptz  not null default now(),
    created_by  varchar(50),
    updated_at  timestamptz,
    updated_by  varchar(50),
    deleted     boolean      not null default false,

    constraint fk_order_items_order   foreign key (order_id) references orders(id),
    constraint fk_order_items_variant foreign key (product_variant_id) references product_variants(id)
);

-- 14. tracking_logs -> users, orders  (moi lan doi status = 1 dong)
create table tracking_logs (
    id          uuid         primary key default gen_random_uuid(),
    user_id     uuid         not null,
    order_id    uuid         not null,
    status      varchar(20)  not null
        constraint chk_tracking_logs_status
            check (status in ('PENDING','CONFIRMED','PICKING','SHIPPING','DELIVERED','CANCELLED','FAILED','RETURNING','REATTEMPT')),
    location    varchar(255),
    note        varchar(500),

    created_at  timestamptz  not null default now(),
    created_by  varchar(50),
    updated_at  timestamptz,
    updated_by  varchar(50),
    deleted     boolean      not null default false,

    constraint fk_tracking_logs_user  foreign key (user_id) references users(id),
    constraint fk_tracking_logs_order foreign key (order_id) references orders(id)
);
