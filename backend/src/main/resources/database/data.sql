-- Provide SQL scripts here
CREATE TABLE parts (
    id UUID PRIMARY KEY,
    sku INTEGER NOT NULL UNIQUE,
    manufacturer VARCHAR(100) NOT NULL,
    name VARCHAR(150) NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_part_price_positive CHECK (price > 0)
);

CREATE TABLE estimates (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    vehicle_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_estimate_status CHECK (status IN ('PENDING', 'APPROVED', 'REFUSED'))
);

CREATE TABLE work_orders (
    id UUID PRIMARY KEY,
    vehicle_id UUID NOT NULL,
    estimate_id UUID,
    status VARCHAR(20) NOT NULL,
    summary VARCHAR(150) NOT NULL,
    notes VARCHAR(1000),
    labor_rate DECIMAL(10, 2) NOT NULL,
    labor_time DECIMAL(8, 2) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_work_orders_estimate FOREIGN KEY (estimate_id) REFERENCES estimates(id),
    CONSTRAINT chk_work_order_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REFUSED')),
    CONSTRAINT chk_labor_rate_positive CHECK (labor_rate > 0),
    CONSTRAINT chk_labor_time_positive CHECK (labor_time > 0)
);

CREATE TABLE work_order_parts (
    id UUID PRIMARY KEY,
    work_order_id UUID NOT NULL,
    part_id UUID NOT NULL,
    quantity INTEGER NOT NULL,
    CONSTRAINT uk_work_order_parts_work_order_part UNIQUE (work_order_id, part_id),
    CONSTRAINT fk_work_order_parts_work_order FOREIGN KEY (work_order_id) REFERENCES work_orders(id),
    CONSTRAINT fk_work_order_parts_part FOREIGN KEY (part_id) REFERENCES parts(id),
    CONSTRAINT chk_work_order_part_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX idx_estimates_customer_id ON estimates(customer_id);
CREATE INDEX idx_estimates_status ON estimates(status);
CREATE INDEX idx_work_orders_estimate_id ON work_orders(estimate_id);
CREATE INDEX idx_work_orders_status ON work_orders(status);

INSERT INTO parts (id, sku, manufacturer, name, price, created_at, updated_at) VALUES
('11111111-1111-1111-1111-111111111111', 41001, 'Bosch', 'QuietCast Brake Pad Set', 89.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('22222222-2222-2222-2222-222222222222', 41002, 'Denso', 'Iridium Spark Plug', 12.50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('33333333-3333-3333-3333-333333333333', 41003, 'Gates', 'Serpentine Belt', 44.75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('44444444-4444-4444-4444-444444444444', 41004, 'Wix', 'Engine Oil Filter', 13.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('55555555-5555-5555-5555-555555555555', 41005, 'Monroe', 'Quick-Strut Assembly', 219.95, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO estimates (id, customer_id, vehicle_id, status, created_at, updated_at) VALUES
('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', '12121212-1212-1212-1212-121212121212', '99999999-9999-9999-9999-999999999999', 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('ffffffff-ffff-ffff-ffff-ffffffffffff', '34343434-3434-3434-3434-343434343434', '88888888-8888-8888-8888-888888888888', 'REFUSED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO work_orders (id, vehicle_id, estimate_id, status, summary, notes, labor_rate, labor_time, created_at, updated_at) VALUES
('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '99999999-9999-9999-9999-999999999999', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'PENDING', 'Replace front brake pads', 'Pads are worn below recommended thickness.', 145.00, 1.50, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '99999999-9999-9999-9999-999999999999', 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'ACCEPTED', 'Tune ignition and belt system', 'Customer approved spark plug and belt replacement.', 155.00, 2.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('cccccccc-cccc-cccc-cccc-cccccccccccc', '88888888-8888-8888-8888-888888888888', 'ffffffff-ffff-ffff-ffff-ffffffffffff', 'REFUSED', 'Replace front strut assemblies', 'Customer declined suspension work for now.', 135.00, 3.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('dddddddd-dddd-dddd-dddd-dddddddddddd', '99999999-9999-9999-9999-999999999999', NULL, 'PENDING', 'Perform diagnostic inspection', 'Labor-only inspection before maintenance visit.', 150.00, 0.75, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO work_order_parts (id, work_order_id, part_id, quantity) VALUES
('10000000-0000-0000-0000-000000000001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '11111111-1111-1111-1111-111111111111', 1),
('10000000-0000-0000-0000-000000000002', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '22222222-2222-2222-2222-222222222222', 4),
('10000000-0000-0000-0000-000000000003', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '33333333-3333-3333-3333-333333333333', 1),
('10000000-0000-0000-0000-000000000004', 'cccccccc-cccc-cccc-cccc-cccccccccccc', '55555555-5555-5555-5555-555555555555', 2);
