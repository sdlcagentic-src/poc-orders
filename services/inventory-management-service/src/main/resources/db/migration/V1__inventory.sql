create table stock_level (
    product_id uuid primary key,
    quantity integer not null,
    constraint stock_level_non_negative check (quantity >= 0)
);

create table reservation (
    reservation_id uuid primary key,
    order_id uuid not null unique,
    reserved_at timestamp with time zone not null
);

create table reservation_item (
    reservation_id uuid not null references reservation (reservation_id),
    product_id uuid not null references stock_level (product_id),
    quantity integer not null check (quantity > 0),
    primary key (reservation_id, product_id)
);
