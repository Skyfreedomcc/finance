-- MySQL dump 10.13  Distrib 8.0.44, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: finance_db
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `finance_account`
--

DROP TABLE IF EXISTS `finance_account`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_account` (
  `account_id` bigint NOT NULL AUTO_INCREMENT,
  `book_id` bigint DEFAULT NULL,
  `parent_id` bigint DEFAULT '0',
  `account_name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `account_type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `account_code` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `balance_direction` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `currency` varchar(10) COLLATE utf8mb4_unicode_ci DEFAULT 'CNY',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`account_id`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `finance_account`
--

LOCK TABLES `finance_account` WRITE;
/*!40000 ALTER TABLE `finance_account` DISABLE KEYS */;
INSERT INTO `finance_account` VALUES (1,NULL,0,'资产','ASSET','1000','DEBIT','CNY',NULL),(2,NULL,0,'负债','LIABILITY','2000','CREDIT','CNY',NULL),(3,NULL,0,'所有者权益','EQUITY','3000','CREDIT','CNY',NULL),(4,NULL,0,'收入','INCOME','4000','CREDIT','CNY',NULL),(5,NULL,0,'费用','EXPENSE','5000','DEBIT','CNY',NULL),(6,NULL,1,'流动资产','ASSET','1100','DEBIT','CNY',NULL),(7,NULL,1,'非流动资产','ASSET','1200','DEBIT','CNY',NULL),(8,NULL,6,'货币资金','ASSET','1001','DEBIT','CNY',NULL),(9,NULL,6,'应收账款','ASSET','1002','DEBIT','CNY',NULL),(10,NULL,6,'存货','ASSET','1003','DEBIT','CNY',NULL),(11,NULL,6,'其他流动资产','ASSET','1004','DEBIT','CNY',NULL),(12,NULL,7,'固定资产','ASSET','1601','DEBIT','CNY',NULL),(13,NULL,7,'累计折旧','ASSET','1602','CREDIT','CNY',NULL),(14,NULL,2,'流动负债','LIABILITY','2100','CREDIT','CNY',NULL),(15,NULL,2,'长期负债','LIABILITY','2200','CREDIT','CNY',NULL),(16,NULL,14,'短期借款','LIABILITY','2201','CREDIT','CNY',NULL),(17,NULL,14,'应付账款','LIABILITY','2202','CREDIT','CNY',NULL),(18,NULL,14,'应付职工薪酬','LIABILITY','2203','CREDIT','CNY',NULL),(19,NULL,14,'其他流动负债','LIABILITY','2209','CREDIT','CNY',NULL),(20,NULL,3,'实收资本','EQUITY','3001','CREDIT','CNY',NULL),(21,NULL,3,'留存收益','EQUITY','3002','CREDIT','CNY',NULL),(22,NULL,3,'本年利润','EQUITY','3003','CREDIT','CNY',NULL),(23,NULL,4,'主营业务收入','INCOME','6001','CREDIT','CNY',NULL),(24,NULL,5,'主营业务成本','EXPENSE','6401','DEBIT','CNY',NULL),(25,NULL,5,'管理费用','EXPENSE','6602','DEBIT','CNY',NULL),(27,NULL,25,'管理费用-工资','EXPENSE','660201','DEBIT','CNY',NULL),(28,NULL,1,'应收账款','ASSET','1122','DEBIT','CNY',NULL),(29,NULL,1,'库存商品','ASSET','1405','DEBIT','CNY',NULL),(30,NULL,25,'管理费用-办公费','EXPENSE','660202','DEBIT','CNY',NULL),(31,NULL,5,'销售费用','EXPENSE','6601','DEBIT','CNY',NULL),(32,NULL,5,'财务费用','EXPENSE','6603','DEBIT','CNY',NULL),(33,NULL,3,'本年利润','EQUITY','3103','CREDIT','CNY',NULL);
/*!40000 ALTER TABLE `finance_account` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `finance_book`
--

DROP TABLE IF EXISTS `finance_book`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_book` (
  `book_id` bigint NOT NULL AUTO_INCREMENT,
  `book_name` varchar(100) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`book_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `finance_book`
--

LOCK TABLES `finance_book` WRITE;
/*!40000 ALTER TABLE `finance_book` DISABLE KEYS */;
INSERT INTO `finance_book` VALUES (1,'2025年主账簿 (默认)','2025-12-06 18:02:39');
/*!40000 ALTER TABLE `finance_book` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `finance_customer`
--

DROP TABLE IF EXISTS `finance_customer`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_customer` (
  `customer_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'CUSTOMER/VENDOR',
  `contact_person` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `address` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`customer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `finance_customer`
--

LOCK TABLES `finance_customer` WRITE;
/*!40000 ALTER TABLE `finance_customer` DISABLE KEYS */;
INSERT INTO `finance_customer` VALUES (1,'中南家具厂','VENDOR','张总',NULL,NULL,'2025-12-06 18:12:17'),(2,'李总','CUSTOMER',NULL,NULL,NULL,'2025-12-06 19:40:00');
/*!40000 ALTER TABLE `finance_customer` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `finance_employee`
--

DROP TABLE IF EXISTS `finance_employee`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_employee` (
  `employee_id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `department` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `position` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `basic_salary` decimal(10,2) DEFAULT NULL,
  PRIMARY KEY (`employee_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `finance_employee`
--

LOCK TABLES `finance_employee` WRITE;
/*!40000 ALTER TABLE `finance_employee` DISABLE KEYS */;
INSERT INTO `finance_employee` VALUES (1,'小王',NULL,NULL,NULL,3000.00);
/*!40000 ALTER TABLE `finance_employee` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `finance_invoice`
--

DROP TABLE IF EXISTS `finance_invoice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_invoice` (
  `invoice_id` bigint NOT NULL AUTO_INCREMENT,
  `customer_id` bigint DEFAULT NULL,
  `invoice_code` varchar(50) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `invoice_date` date DEFAULT NULL,
  `due_date` date DEFAULT NULL,
  `total_amount` decimal(15,2) DEFAULT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`invoice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `finance_invoice`
--

LOCK TABLES `finance_invoice` WRITE;
/*!40000 ALTER TABLE `finance_invoice` DISABLE KEYS */;
/*!40000 ALTER TABLE `finance_invoice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `finance_invoice_item`
--

DROP TABLE IF EXISTS `finance_invoice_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_invoice_item` (
  `item_id` bigint NOT NULL AUTO_INCREMENT,
  `invoice_id` bigint NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `quantity` decimal(10,2) DEFAULT NULL,
  `unit_price` decimal(15,2) DEFAULT NULL,
  `line_amount` decimal(15,2) DEFAULT NULL,
  PRIMARY KEY (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `finance_invoice_item`
--

LOCK TABLES `finance_invoice_item` WRITE;
/*!40000 ALTER TABLE `finance_invoice_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `finance_invoice_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `finance_split`
--

DROP TABLE IF EXISTS `finance_split`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_split` (
  `split_id` bigint NOT NULL AUTO_INCREMENT,
  `transaction_id` bigint NOT NULL,
  `account_id` bigint NOT NULL,
  `dc_direction` int DEFAULT NULL,
  `amount` decimal(15,2) NOT NULL,
  `reconcile_state` char(1) COLLATE utf8mb4_unicode_ci DEFAULT 'n',
  `summary` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`split_id`)
) ENGINE=InnoDB AUTO_INCREMENT=27 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `finance_split`
--

LOCK TABLES `finance_split` WRITE;
/*!40000 ALTER TABLE `finance_split` DISABLE KEYS */;
INSERT INTO `finance_split` VALUES (1,1,9,1,1000000.00,'n','初始注资'),(2,1,20,-1,1000000.00,'n','老板投资'),(14,7,10,1,1560.00,'n','桌子 (单价:52 × 数量:30)'),(15,7,10,1,880.00,'n','柜子 (单价:10 × 数量:88)'),(16,7,17,-1,2440.00,'n','应付供应商款项'),(17,8,9,1,25000.00,'n','应收客户款项'),(18,8,23,-1,25000.00,'n','小零食 (单价:50 × 数量:500)'),(19,9,27,1,3500.00,'n','2025-12 员工工资'),(20,9,8,-1,3500.00,'n','2025-12 工资发放'),(21,10,29,1,560.00,'n','水杯20'),(22,10,17,-1,560.00,'n','水杯20'),(23,11,29,1,1560.00,'n','文件夹 (30×52)'),(24,11,17,-1,1560.00,'n','应付供应商'),(25,12,17,1,560.00,'n','付款 - 采购办公用品'),(26,12,8,-1,560.00,'n','付款 - 采购办公用品');
/*!40000 ALTER TABLE `finance_split` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `finance_transaction`
--

DROP TABLE IF EXISTS `finance_transaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_transaction` (
  `transaction_id` bigint NOT NULL AUTO_INCREMENT,
  `book_id` bigint DEFAULT NULL,
  `voucher_date` date NOT NULL,
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT 'DRAFT' COMMENT '状态: DRAFT-草稿, POSTED-已过账',
  PRIMARY KEY (`transaction_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `finance_transaction`
--

LOCK TABLES `finance_transaction` WRITE;
/*!40000 ALTER TABLE `finance_transaction` DISABLE KEYS */;
INSERT INTO `finance_transaction` VALUES (1,1,'2025-12-09','初始化-老板注资','2025-12-09 23:14:03','POSTED'),(7,1,'2025-12-10','采购入库 - ','2025-12-10 13:43:59','POSTED'),(8,NULL,'2025-12-10','销售出库 - ','2025-12-10 19:04:41','POSTED'),(9,NULL,'2025-12-10','2025-12 工资发放','2025-12-10 19:53:15','POSTED'),(10,NULL,'2025-12-11','采购办公用品','2025-12-11 08:06:46','POSTED'),(11,NULL,'2025-12-11','采购入库 - ','2025-12-11 08:31:15','POSTED'),(12,NULL,'2025-12-12','付款 - 采购办公用品','2025-12-12 14:15:27','POSTED');
/*!40000 ALTER TABLE `finance_transaction` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'admin','123456','BOSS','2025-12-06 18:02:39');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-18 20:46:14
