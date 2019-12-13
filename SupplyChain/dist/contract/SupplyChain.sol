pragma solidity ^0.4.24;

import "./Table.sol";

contract SupplyChain{

    //event
    event CreateEvent(int ret, string fromAccount, string toAccount, string item, uint amount);
    event PassEvent(int ret, string issuer, string from_account, string from_item, string to_account, string to_item, uint amount);
    event BankEvent(int ret, string from_account, string to_account, string item);
    event RedeemEvent(int ret, string from_account, string to_account, string item);
    
        // 构造函数中创建t_receipt
    constructor() {
        createTable();
    }
    
    function createTable() private {
        TableFactory tf = TableFactory(0x1001); 
        
        // 创建表
        tf.createTable("t_receipt", "from_account", "to_account, item, amount");
    }

    function openTable() private returns(Table) {
        TableFactory tf = TableFactory(0x1001);
        Table table = tf.openTable("t_receipt");
        return table;
    }
    
    function getReceipt(string from_account, string to_account, string item) public constant returns(int, uint){
        // 打开表
        Table table = openTable();
        // 查询
        Condition condition = table.newCondition();
        condition.EQ("to_account", to_account);
        condition.EQ("item", item);
        Entries entries = table.select(from_account, condition);
        uint amount = 0;
        if (0 == uint(entries.size())) {
            return (-1, 0);
        } else {
            Entry entry = entries.get(0);
            return (0, uint256(entry.getInt("amount")));
        }
    }
    
    function createReceipt(string from_account, string to_account, string item, uint amount) public returns(int){
        int ret = 0;
        uint temp_amount = 0;
        int ret_code = 0;
        
        Table table = openTable();
        (ret, temp_amount) = getReceipt(from_account, to_account, item);
        if(ret != 0){
            Entry newEntry = table.newEntry();
            newEntry.set("from_account", from_account);
            newEntry.set("to_account", to_account);
            newEntry.set("item", item);
            newEntry.set("amount", int256(amount));
            
            int insert_code = table.insert(from_account, newEntry);
            if(insert_code == 1){
                ret_code = 0;
            }
            else{
                ret_code = -1;
            }
        }
        else{
            Condition condition = table.newCondition();
            condition.EQ("to_account", to_account);
            condition.EQ("item", item);
            Entry updateEntry = table.newEntry();
            updateEntry.set("from_account", from_account);
            updateEntry.set("to_account", to_account);
            updateEntry.set("item", item);
            updateEntry.set("amount", int256(amount + temp_amount));
            
            int update_code = table.update(from_account, updateEntry, condition);
            if(update_code == 1){
                ret_code = 0;
            }
            else{
                ret_code = -1;
            }
        }

        emit CreateEvent(ret_code, from_account, to_account, item, amount);
        
        return ret_code;
    }
    
    function passReceipt(string issuer, string from_account, string from_item, string to_account, string to_item, uint amount) public returns(int){
        int ret = 0;
        uint temp_amount = 0;
        int ret_code = 0;
        
        (ret, temp_amount) = getReceipt(from_account, issuer, from_item);
        if(ret == 0 && temp_amount >= amount){

           if(createReceipt(from_account, to_account, to_item, amount) == -1){
               ret_code = -3;
           }
           else{
               Table table = openTable();
               
               Condition condition = table.newCondition();
               condition.EQ("to_account", issuer);
               condition.EQ("item", from_item);
               Entry updateEntry = table.newEntry();
               updateEntry.set("from_account", from_account);
               updateEntry.set("to_account", issuer);
               updateEntry.set("item", from_item);
               updateEntry.set("amount", int256(temp_amount - amount));
               
               if(table.update(from_account, updateEntry, condition) == -1){
                   ret_code = -3;
               }
               else{
                   ret_code = 0;
               }
           }
        }
        else{
            if(ret == -1){
                ret_code = -1;
            }
            else if(temp_amount < amount){
                ret_code = -2;
            }
        }

        emit PassEvent(ret_code, issuer, from_account, from_item, to_account, to_item, amount);
        
        return ret_code;
    }
    
    function receiptToBank(string from_account, string to_account, string item) public returns(int){
        int ret = 0;
        uint temp_amount = 0;
        int ret_code = 0;
        
        (ret, temp_amount) = getReceipt(from_account, to_account, item);
        if(ret == 0){
            int bank_code = createReceipt(to_account, "bank", item, temp_amount);
            if(bank_code != 0){
                ret_code = -2;
            }
            else{
                ret_code = 0;
            }
        }
        else{
            ret_code = -1;
        }

        emit BankEvent(ret_code, from_account, to_account, item);
        
        return ret_code;
    }
    
    function redeemReceipt(string from_account, string to_account, string item) public returns(int){
        int ret = 0;
        uint temp_amount = 0;
        int ret_code = 0;
        
        (ret, temp_amount) = getReceipt(from_account, to_account, item);
        if(ret == 0){
            Table table = openTable();
            Condition condition = table.newCondition();
            condition.EQ("to_account", to_account);
            condition.EQ("item", item);
            int redeem_code = table.remove(from_account, condition);
            if(redeem_code == 0){
                ret_code = 0;
            }
            else{
                ret_code = -2;
            }
        }
        else{
            ret_code = -1;
        }

        emit RedeemEvent(ret_code, from_account, to_account, item);
        
        return ret_code;
    }
}
