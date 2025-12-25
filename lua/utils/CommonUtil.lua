-- 执行修改sql，如果返回为0则成功，1则为失败
function execGaussUpdateSql(sql)
    local success, result = pcall(function()
        return ExecuteGaussSqlHelper:executeUpdateSql(sql)
    end)
    if not success then
        executionResult = {
            status = "1",
            message = "脚本执行成功"
        }
        error("sql:" .. sql .. "执行失败:" .. tostring(result))
    else
        return result  -- 返回查询结果
    end
end

-- 执行查询sql，如果返回为0则成功，1则为失败
function executeGaussQuerySql(sql)
    local success, result = pcall(function()
        return ExecuteGaussSqlHelper:executeQuerySql(sql)
    end)
    if not success then
        executionResult = {
            status = "1",
            message = "脚本执行成功"
        }
        error("sql:" .. sql .. "执行失败:" .. tostring(result))
    else
        return result  -- 返回查询结果
    end
end


-- 添加日志，如果返回为0则成功，1则为失败
function addLog(logStr)
    local success, result = pcall(function()
        return ExecuteLuaScriptHelper:addLog(logStr)
    end)
    if not success then
        executionResult = {
            status = "1",
            message = "日志插入失败"
        }
        print("日志插入失败:" .. tostring(result))
        error("日志插入失败:" .. tostring(result))
    end
end

-- 使用Java线程池，执行function
function execAsync(task)
    print(type(task))
    -- 使用Java线程池异步执行SQL
    AsyncCall("通用异步线程", 0, task)
end