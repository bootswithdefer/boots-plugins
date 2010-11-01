CREATE TABLE `blocks` (
  `date` datetime NOT NULL default '0000-00-00 00:00:00',
  `player` varchar(32) NOT NULL default '-',
  `action` varchar(32) NOT NULL default 'none',
  `type` int(11) NOT NULL default '0',
  `x` int(11) NOT NULL default '0',
  `y` int(11) NOT NULL default '0',
  `z` int(11) NOT NULL default '0',
   KEY `coords` (`y`, `x`, `z`));
