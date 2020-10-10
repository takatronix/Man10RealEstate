create table region
(
    id         int auto_increment
        primary key,
    server     varchar(16)  not null comment 'サーバー名',
    world      varchar(16)  not null comment 'ワールド名',
    owner_uuid varchar(36)  null comment '所有者のUUID nullならだれも所有していない',
    owner_name varchar(16)  null comment '所有者の名前',
    x          double       not null comment 'テレポートすると飛ぶ場所',
    y          double       not null comment 'テレポートすると飛ぶ場所',
    z          double       not null comment 'テレポートすると飛ぶ場所',
    pitch      double       not null comment 'テレポートすると飛ぶ場所',
    yaw        double       not null comment 'テレポートすると飛ぶ場所',
    sx         double       null comment '始点',
    sy         double       null comment '始点',
    sz         double       null comment '始点',
    ex         double       null comment '終点',
    ey         double       null comment '終点',
    ez         double       null comment '終点',
    name       varchar(128) null comment '名称',
    created    datetime     not null default now() comment '作成日',
    status     varchar(16)  not null DEFAULT "OnSale" comment 'OnSale' 販売中 ：保護あり（admin,ownerのみ）
"Free" 保護なし だれでもいじられるエリア
"Protected" 保護あり :　admin,owner,userのみ
"Lock" 違法などでロック中：　admin以外いじれない

',
    price      double       null comment '販売金額
OnSale状態になったときに販売価格
売り上げ金額は、売上テーブルに登録しオフラインでも売買できるとする
',
    profit     double       not null default 0.0 comment '土地の利益',
--    rent       double       not null default 0.0 comment '賃料',
    span       Int          null comment '支払うスパン(0:moth 1:week 2:day)',
    remit_tax  tinyint      not null default 0   comment  '税金の免除をするかどうか'
)
    comment '領域管理のためのテーブル';

