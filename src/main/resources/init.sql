-- Таблиця категорій / типів стратегій (Батьківська сутність, Рівень 2)
CREATE TABLE IF NOT EXISTS categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- Основна таблиця сутностей (Зв'язок Many-To-One через Foreign Key з CASCADE)
CREATE TABLE IF NOT EXISTS warehouse_items (
    id BIGINT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    sku VARCHAR(50) NOT NULL UNIQUE,
    quantity INT NOT NULL CHECK (quantity >= 0),
    price NUMERIC(15, 2) NOT NULL CHECK (price >= 0.00),
    category_id BIGINT,
    item_type VARCHAR(50) NOT NULL,
    weight_kg NUMERIC(10, 2),
    expiration_date DATE,
    warranty_months INT,
    strategy_type VARCHAR(50) DEFAULT 'REGULAR',
    discount_percentage NUMERIC(5, 2) DEFAULT 0.00,
    CONSTRAINT fk_category FOREIGN KEY (category_id) REFERENCES categories(id) ON DELETE CASCADE
);