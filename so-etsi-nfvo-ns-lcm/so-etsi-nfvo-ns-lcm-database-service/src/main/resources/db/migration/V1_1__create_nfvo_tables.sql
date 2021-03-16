use nfvo;

CREATE TABLE IF NOT EXISTS `JOB` (
  `JOB_ID` varchar(255) NOT NULL,
  `JOB_TYPE` varchar(255) NOT NULL,
  `JOB_ACTION` varchar(255) NOT NULL,
  `RESOURCE_ID` varchar(255) NOT NULL,
  `RESOURCE_NAME` varchar(255) DEFAULT NULL,
  `STATUS` varchar(255) NOT NULL,
  `START_TIME` DATETIME DEFAULT NULL,
  `END_TIME` DATETIME DEFAULT NULL,
  `PROGRESS` int(11) DEFAULT NULL,
  `PROCESS_INSTANCE_ID` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`JOB_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;



CREATE TABLE IF NOT EXISTS `JOB_STATUS` (
  `ID` INT NOT NULL AUTO_INCREMENT,
  `UPDATED_TIME` DATETIME NOT NULL,
  `DESCRIPTION` longtext DEFAULT NULL,
  `STATUS` varchar(255) NOT NULL,
  `JOB_ID` varchar(255) NOT NULL,
  PRIMARY KEY (`ID`),
  FOREIGN KEY (JOB_ID)
      REFERENCES JOB(JOB_ID)
      ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `NS_INST` (
  `NS_INST_ID` varchar(255) NOT NULL,
  `NAME` varchar(255) NOT NULL,
  `NS_PACKAGE_ID` varchar(255) DEFAULT NULL,
  `NSD_ID` varchar(255) NOT NULL,
  `NSD_INVARIANT_ID` varchar(255) NOT NULL,
  `DESCRIPTION` longtext DEFAULT NULL,
  `STATUS` varchar(255) NOT NULL,
  `STATUS_UPDATED_TIME` DATETIME NOT NULL,
  `GLOBAL_CUSTOMER_ID` varchar(255) DEFAULT NULL,
  `SERVICE_TYPE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`NS_INST_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `NF_INST` (
  `NF_INST_ID` varchar(255) NOT NULL,
  `NAME` varchar(255) DEFAULT NULL,
  `VNFD_ID` varchar(255) DEFAULT NULL,
  `PACKAGE_ID` varchar(255) DEFAULT NULL,
  `NS_INST_ID` varchar(255) NOT NULL,
  `STATUS` varchar(255) NOT NULL,
  `CREATE_TIME` DATETIME DEFAULT NULL,
  `LAST_UPDATE_TIME` DATETIME DEFAULT NULL,
  PRIMARY KEY (`NF_INST_ID`),
  FOREIGN KEY (NS_INST_ID)
      REFERENCES NS_INST(NS_INST_ID)
	  ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE IF NOT EXISTS `NS_LCM_OP_OCCS` (
  `ID` varchar(255) NOT NULL,
  `OPERATION_STATE` varchar(255) NOT NULL,
  `STATE_ENTERED_TIME` DATETIME DEFAULT NULL,
  `START_TIME` DATETIME DEFAULT NULL,
  `NS_INST_ID` varchar(255) NOT NULL,
  `OPERATION` varchar(255) NOT NULL,
  `IS_AUTO_INVOCATION` varchar(255) NOT NULL,
  `OPERATION_PARAMS` longtext NOT NULL,
  `IS_CANCEL_PENDING` varchar(255) NOT NULL,
  `CANCEL_MODE` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`ID`),
  FOREIGN KEY (NS_INST_ID)
      REFERENCES NS_INST(NS_INST_ID)
	  ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8;