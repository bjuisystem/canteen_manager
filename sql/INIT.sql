-- Seed data for canteen_management

USE canteen_management;

INSERT INTO myUser (name, username, password, sex, telephone, department, workStation, role)
VALUES
    ('Manager', 'manager', SHA2('123456', 256), 'M', '15700000001', 'Admin', 'B1-101', 'manager'),
    ('Chef', 'chef', SHA2('123456', 256), 'M', '15700000002', 'Kitchen', 'B1-201', 'chef'),
    ('Caterer', 'caterer', SHA2('123456', 256), 'F', '15700000003', 'Delivery', 'B1-301', 'caterer'),
    ('Treasurer', 'treasurer', SHA2('123456', 256), 'F', '15700000004', 'Finance', 'B1-401', 'treasurer'),
    ('Staff', 'staff', SHA2('123456', 256), 'F', '15700000005', 'R&D', 'A3-502', 'staff')
ON DUPLICATE KEY UPDATE telephone=VALUES(telephone), department=VALUES(department), workStation=VALUES(workStation), role=VALUES(role);

INSERT INTO recipe (name, category, unit, price, description)
VALUES
    ('Fried Rice', 'Main', 'plate', 12.00, 'Classic fried rice'),
    ('Beef Noodles', 'Main', 'bowl', 18.00, 'Beef noodles with soup'),
    ('Spring Rolls', 'Snack', 'plate', 8.00, 'Crispy rolls'),
    ('Lemon Tea', 'Drink', 'cup', 6.00, 'Fresh lemon tea')
ON DUPLICATE KEY UPDATE price=VALUES(price), description=VALUES(description);

INSERT INTO menu (name, category, picture, unit, price, createTime, recipeId)
SELECT r.name, r.category, NULL, r.unit, r.price, NOW(), r.recipeId
FROM recipe r
ON DUPLICATE KEY UPDATE price=VALUES(price), createTime=VALUES(createTime);

INSERT INTO time_config (order_deadline, meal_start_time, update_time)
VALUES ('09:00:00', '11:30:00', NOW())
ON DUPLICATE KEY UPDATE order_deadline=VALUES(order_deadline), meal_start_time=VALUES(meal_start_time), update_time=VALUES(update_time);
