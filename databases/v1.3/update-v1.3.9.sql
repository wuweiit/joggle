
ALTER TABLE `client_version`
    ADD COLUMN `type` varchar(10) NULL COMMENT '类型  CLIENT SERVER' AFTER `id`;


update client_version set type = 'CLIENT'  where title like 'JoggleClient%';