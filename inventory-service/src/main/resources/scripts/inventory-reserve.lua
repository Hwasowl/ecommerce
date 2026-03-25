for i, key in ipairs(KEYS) do
    local stock = redis.call('GET', key)
    if not stock then
        return 'FAIL|PRODUCT_NOT_FOUND|재고 정보를 찾을 수 없습니다.'
    end

    stock = tonumber(stock)
    local quantity = tonumber(ARGV[i])
    if stock < quantity then
        return 'FAIL|INSUFFICIENT_STOCK|재고가 부족합니다.'
    end
end

local results = {}
for i, key in ipairs(KEYS) do
    local beforeStock = tonumber(redis.call('GET', key))
    local quantity = tonumber(ARGV[i])
    local afterStock = beforeStock - quantity

    redis.call('SET', key, afterStock)
    table.insert(results, tostring(beforeStock))
    table.insert(results, tostring(afterStock))
end

return 'OK|' .. table.concat(results, ',')
