-- 如果返回为0则成功，1则为失败
local function execUpdateSql(sql)
    local success, result = pcall(function()
        return ExecuteGaussSqlHelper:executeUpdateSql(sql)
    end)
    if not success then
        executionResult = {
            status = "1",
            message = "脚本执行成功"
        }
        error("sql:" .. sql .. "执行失败:" .. result)
    end
end

-- 主逻辑
local function main()
    print("开始执行客户数据导入流程...")

    -- 1. 创建临时表（如果不存在），结构同模板表
    execUpdateSql("DROP TABLE IF EXISTS fmds_custominfo_tmp")
    execUpdateSql("CREATE TABLE fmds_custominfo_tmp AS SELECT * FROM fmds_custominfo_tpl")

    -- 2. 对源表姓名脱敏：保留首字，其余替换为 *
    execUpdateSql([[
        UPDATE nyyh_src_g_pf_idv_cust_info_bdci
        SET idv_cust_nam = CONCAT(
            LEFT(idv_cust_nam, 1),
            REPEAT('*', CHAR_LENGTH(idv_cust_nam) - 1)
        )
    ]])

    -- 3. 将脱敏后的客户数据插入临时表
    execUpdateSql([[
        MERGE INTO fmds_custominfo_tmp AS target
        USING (
            SELECT
                idv_cust_cod,
                '0' AS uid,
                idv_cust_nam AS name,
                '' AS phone,
                idv_cust_sex AS sex,
                CASE
                    WHEN TO_DATE('20251222', 'YYYYMMDD') > TO_DATE('1920-01-01', 'YYYY-MM-DD') THEN
                        EXTRACT(DAY FROM (CURRENT_DATE - TO_DATE('20251222', 'YYYYMMDD'))) / 365.0
                    ELSE 0
                    END AS age,
                '' AS job,
                '0' AS groupid,
                '' AS sfrom,
                '0' AS assets,
                '0' AS mostlost,
                '0' AS risklevel,
                idv_cust_cod AS bankcustomid,
                idv_cust_std_org_no AS deptcode,
                '' AS tzyx,
                '0' AS yxzj,
                '0' AS remark,
                CURRENT_DATE AS createtime
            FROM nyyh_src_g_pf_idv_cust_info_bdci
        ) AS source
        ON (target.cid = source.idv_cust_cod)
        WHEN MATCHED THEN
            UPDATE SET
                       uid = source.uid,
                       name = source.name,
                       phone = source.phone,
                       sex = source.sex,
                       age = source.age,
                       job = source.job,
                       groupid = source.groupid,
                       sfrom = source.sfrom,
                       assets = source.assets,
                       mostlost = source.mostlost,
                       risklevel = source.risklevel,
                       bankcustomid = source.bankcustomid,
                       deptcode = source.deptcode,
                       tzyx = source.tzyx,
                       yxzj = source.yxzj,
                       remark = source.remark,
                       createtime = source.createtime
        WHEN NOT MATCHED THEN
            INSERT (cid, uid, name, phone, sex, age, job, groupid, sfrom, assets, mostlost, risklevel, bankcustomid, deptcode, tzyx, yxzj, remark, createtime)
            VALUES (source.idv_cust_cod, source.uid, source.name, source.phone, source.sex, source.age, source.job, source.groupid, source.sfrom, source.assets, source.mostlost, source.risklevel, source.bankcustomid, source.deptcode, source.tzyx, source.yxzj, source.remark, source.createtime)
    ]])

    -- 4. 更新管户信息（关联客户经理）
    execUpdateSql([[
        UPDATE fmds_custominfo_tmp a
        SET uid = c.ssoid
        FROM nyyh_src_ocrm_cust_magr_dire_rln b, nyyh_src_ocrm_sf_tab c
        WHERE a.cid = b.pid
          AND b.uid = c.uid
          AND b.uid > 0
    ]])

    -- 5. 更新机构信息（映射机构代码）
    execUpdateSql([[
        UPDATE fmds_custominfo_tmp a
        SET uid = c.ssoid
        FROM nyyh_src_ocrm_cust_magr_dire_rln b, nyyh_src_ocrm_sf_tab c
        WHERE a.cid = b.pid
          AND b.uid = c.uid
          AND b.uid > 0
    ]])

    print("客户数据导入完成！")

    os.execute("bash /path/to/your/script.sh")

    -- 存储执行结果
    executionResult = {
        status = "0",
        message = "脚本执行成功",
        selectResult = tostring(selectRes)
    }

end

main()