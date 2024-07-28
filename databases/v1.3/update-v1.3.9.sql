
ALTER TABLE `client_version`
    ADD COLUMN `type` varchar(10) NULL COMMENT '类型  CLIENT SERVER' AFTER `id`;


update client_version set type = 'CLIENT'  where title like 'JoggleClient%';



ALTER TABLE `t_server_tunnel`
    ADD COLUMN `version` varchar(20) NULL COMMENT '通道版本' AFTER `server_addr`;