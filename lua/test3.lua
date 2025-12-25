-- main.lua
--dofile("CommonUtil.lua")  -- 执行 config.lua
dofile(projectRoot .. "/lua/utils/CommonUtil.lua")  -- 执行 config.lua


-- 主逻辑
local function main()
    print("开始执行客户数据导入流程...")

    -- 1. 删除临时表
    local dropResult = execGaussUpdateSql("DROP TABLE IF EXISTS fmds_custominfo_tmp")

    print("删除结果: " .. tostring(dropResult) .. ",type:" .. type(dropResult))

    -- 2. 创建临时表
    local createResult = execGaussUpdateSql("CREATE TABLE fmds_custominfo_tmp AS SELECT * FROM fmds_custominfo_tpl")

    print("创建结果: " .. tostring(createResult) .. ",type:" .. type(createResult))
    -- 3.执行查询数据并打印结果
    local queryResultList = executeGaussQuerySql("SELECT * FROM nyyh_src_g_pf_idv_cust_info_bdci LIMIT 3 OFFSET 0")

    if queryResultList then
        print("查询结果:")
        -- 如果是表结构，遍历打印
        if type(queryResultList) == "table" then
            for i, row in ipairs(queryResultList) do
                print("行 " .. i .. ": " .. tostring(row))
            end
        else
            print(tostring(queryResultList))
        end
    else
        print("查询结果为空")
    end

    print("客户数据导入完成！")

    -- 4.执行shell脚本
    local cmd1 = os.execute("bash C:/disk/project/work/2025/nbatch/lua/test_shell1.sh")

    local cmd2 = os.execute("echo hello, shell!")

    -- 5.执行异步函数
    execAsync(function()
        for i = 1, 5 do
            print("⏳ 异步线程继续工作... " .. i)
            -- 使用Lua的原生延时函数替代系统调用，避免阻塞和安全问题
            local start = os.clock()
        end
    end)

    print("主线程结束...")

    -- 6.在执行过程当中写入日志
    addLog("插入日志sql测试")

    -- 存储执行结果
    executionResult = {
        status = "0",
        message = "脚本执行成功",
        selectResult = tostring(selectRes)
    }

end

main()