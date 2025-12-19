-- 如果返回为0则成功，1则为失败
local function exec_update_sql(sql)
    local success, result = pcall(function()
        return ExecuteGaussSqlHelper:executeUpdateSql(sql)
    end)
    if not success then
        executionResult = {
            status = "1",
            message = "脚本执行成功"
        }
        error("sql:" + sql + "执行失败:" + result)
    end
end


-- 主逻辑
local function main()
    -- 2. 创建并清空临时表
    exec_update_sql("update nyyh_src_g_pf_idv_cust_info_bdci set fncl_ent_no = '0' where idv_cust_cod = '1'")

    -- 存储执行结果
    executionResult = {
        status = "0",
        message = "脚本执行成功",
        selectResult = tostring(selectRes)
    }
end

-- 执行主函数
main()
