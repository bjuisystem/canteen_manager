-- MySQL schema for canteen management

CREATE DATABASE IF NOT EXISTS canteen_management
DEFAULT CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;

USE canteen_management;

CREATE TABLE IF NOT EXISTS myUser (
    userId      BIGINT AUTO_INCREMENT,
    name        VARCHAR(25) NOT NULL,
    username    VARCHAR(25) NOT NULL,
    password    VARCHAR(64) NOT NULL,
    sex         VARCHAR(10) NOT NULL,
    telephone   VARCHAR(20) NOT NULL,
    department  VARCHAR(25) NOT NULL,
    workStation VARCHAR(50) NOT NULL,
    role        VARCHAR(25) NOT NULL,
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (userId),
    UNIQUE KEY UK_MYUSER_USERNAME (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS recipe (
    recipeId    BIGINT AUTO_INCREMENT,
    name        VARCHAR(25) NOT NULL,
    category    VARCHAR(25) NOT NULL,
    picture     VARCHAR(100),
    unit        VARCHAR(10) NOT NULL,
    price       DECIMAL(18,2) NOT NULL,
    description VARCHAR(200) NOT NULL,
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (recipeId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS menu (
    menuId      BIGINT AUTO_INCREMENT,
    name        VARCHAR(25) NOT NULL,
    category    VARCHAR(25) NOT NULL,
    picture     VARCHAR(100),
    unit        VARCHAR(10) NOT NULL,
    price       DECIMAL(18,2) NOT NULL,
    createTime  DATETIME NOT NULL,
    recipeId    BIGINT,
    PRIMARY KEY (menuId),
    CONSTRAINT FK_MENU_RECIPEID FOREIGN KEY (recipeId) REFERENCES recipe(recipeId) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS history (
    hisId       BIGINT AUTO_INCREMENT,
    timeRange   VARCHAR(50) NOT NULL,
    menuIds     VARCHAR(500) NOT NULL,
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (hisId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS orderForm (
    orderId     BIGINT,
    userId      BIGINT NOT NULL,
    name        VARCHAR(25) NOT NULL,
    telephone   VARCHAR(20) NOT NULL,
    workStation VARCHAR(50) NOT NULL,
    orderTime   DATETIME NOT NULL,
    mealDate    DATE NOT NULL,
    orderPrice  DECIMAL(18,2) NOT NULL,
    status      VARCHAR(20) DEFAULT 'PENDING',
    PRIMARY KEY (orderId),
    CONSTRAINT FK_ORDERFORM_USERID FOREIGN KEY (userId) REFERENCES myUser(userId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS blanketOrder (
    mealId      BIGINT AUTO_INCREMENT,
    orderId     BIGINT NOT NULL,
    name        VARCHAR(25) NOT NULL,
    unit        VARCHAR(10) NOT NULL,
    weight      DECIMAL(8,2),
    price       DECIMAL(18,2) NOT NULL,
    quantity    INT NOT NULL DEFAULT 1,
    totalPrice  DECIMAL(18,2) NOT NULL,
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP,
    mealDate    DATE NOT NULL,
    PRIMARY KEY (mealId),
    CONSTRAINT FK_BO_ORDERID FOREIGN KEY (orderId) REFERENCES orderForm(orderId) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS shopCart (
    scId        BIGINT AUTO_INCREMENT,
    userId      BIGINT NOT NULL,
    menuId      BIGINT,
    name        VARCHAR(25) NOT NULL,
    unit        VARCHAR(10) NOT NULL,
    weight      DECIMAL(8,2),
    price       DECIMAL(18,2) NOT NULL,
    quantity    INT NOT NULL DEFAULT 1,
    totalPrice  DECIMAL(18,2) NOT NULL,
    picture     VARCHAR(100),
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updateTime  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (scId),
    CONSTRAINT FK_SC_USERID FOREIGN KEY (userId) REFERENCES myUser(userId) ON DELETE CASCADE,
    CONSTRAINT FK_SC_MENUID FOREIGN KEY (menuId) REFERENCES menu(menuId) ON DELETE SET NULL,
    UNIQUE KEY UK_SC_USER_MENU (userId, menuId)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS sale (
    saleId      BIGINT AUTO_INCREMENT,
    month       VARCHAR(7) NOT NULL,
    totalPrice  DECIMAL(18,2) NOT NULL DEFAULT 0.00,
    createTime  DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (saleId),
    UNIQUE KEY UK_SALE_MONTH (month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS time_config (
    id              BIGINT AUTO_INCREMENT,
    order_deadline  VARCHAR(8) NOT NULL,
    meal_start_time VARCHAR(8) NOT NULL,
    update_time     DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
