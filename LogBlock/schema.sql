CREATE TABLE `blocks` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `date` datetime NOT NULL DEFAULT '0000-00-00 00:00:00',
  `player` varchar(32) NOT NULL DEFAULT '-',
  `replaced` int(11) NOT NULL DEFAULT '0',
  `type` int(11) NOT NULL DEFAULT '0',
  `x` int(11) NOT NULL DEFAULT '0',
  `y` int(11) NOT NULL DEFAULT '0',
  `z` int(11) NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `coords` (`y`,`x`,`z`),
  KEY `type` (`type`),
  KEY `replaced` (`replaced`),
  KEY `player` (`player`)
);

CREATE TABLE `extra` (
  `id` int(11) NOT NULL,
  `extra` text,
  PRIMARY KEY (`id`)
);
