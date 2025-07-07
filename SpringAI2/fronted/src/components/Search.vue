<template>
    <div class="search-container">
        <div class="search-input">
            <input type="text" placeholder="人员编号" v-model="personId" />
            <input type="text" placeholder="姓名" v-model="name" />
            <select v-model="type">
                <option value="1">履历材料</option>
                <option value="2">自传材料</option>
                <option value="3">考察考核材料</option>
                <option value="4">学历职称材料</option>
                <option value="5">政审材料</option>
                <option value="6">党团材料</option>
                <option value="7">奖励材料</option>
                <option value="8">处分材料</option>
                <option value="9">工资职务材料</option>
                <option value="10">其他材料</option>
            </select>
            <input type="text" placeholder="查询内容" v-model="searchContent" />
            <button @click="search">Search</button>
        </div>
    </div>
</template>

<script>
import axios from 'axios';

export default {
    name: 'Search',
    data() {
        return {
            personId: '',//人员编号
            name: '',//姓名
            type: '',//材料类型
            searchContent: ''//查询内容
        }
    },

    methods: {
        async search() {
            //请求体，将人名、材料类型、查询内容发送给后端，后端根据这些参数进行查询
            const params = {
                personId: this.personId,//人员编号
                name: this.name,//姓名
                type: this.type,//材料类型
                searchContent: this.searchContent//查询内容
            }

            //将请求参数发送给后端
            axios.post('http://localhost:8080/api/search/searchResource', params)
                .then(res => {
                    console.log(res);
                })
                .catch(err => {
                    console.log(err);
                });

        }
    }
}
</script>