<template>
  <div>
    <div class="admin-header"><h1 class="admin-title">敏感词管理</h1></div>
    <div style="margin-bottom:16px;">
      <button class="btn btn-primary" @click="openAdd">新增敏感词</button>
    </div>
    <div class="admin-table-container">
      <table class="table">
        <thead><tr><th>ID</th><th>敏感词</th><th>创建时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="w in words" :key="w.id">
            <td>{{ w.id }}</td><td>{{ w.word }}</td><td>{{ w.createTime }}</td>
            <td><div class="table-actions">
              <a href="javascript:void(0)" class="btn btn-primary btn-sm" @click="openEdit(w)">编辑</a>
              <a href="javascript:void(0)" class="btn btn-danger btn-sm" @click="handleDelete(w.id)">删除</a>
            </div></td>
          </tr>
        </tbody>
      </table>
    </div>
    <el-dialog :title="editId ? '编辑敏感词' : '新增敏感词'" :visible.sync="dialogVisible" width="400px">
      <el-input v-model="wordInput" placeholder="输入敏感词" />
      <span slot="footer"><el-button @click="dialogVisible=false">取消</el-button><el-button type="primary" @click="submit">保存</el-button></span>
    </el-dialog>
  </div>
</template>
<script>
import request from '@/api/request'
export default {
  name:'AdminSensitiveWord',
  data(){ return { words:[], dialogVisible:false, wordInput:'', editId:null }},
  mounted(){ this.load() },
  methods:{
    async load(){ const r=await request.get('/admin/comment/words'); if(r.code===200) this.words=r.data.map((w,i)=>({id:i,word:w})) },
    openAdd(){ this.editId=null; this.wordInput=''; this.dialogVisible=true },
    openEdit(w){ this.editId=w.id; this.wordInput=w.word; this.dialogVisible=true },
    async submit(){
      if(!this.wordInput.trim()) return
      if(this.editId!=null){ await request.put(`/admin/comment/words/${this.editId}`,{word:this.wordInput.trim()}) }
      else { await request.post('/admin/comment/words',{word:this.wordInput.trim()}) }
      this.$message.success('保存成功'); this.dialogVisible=false; this.load()
    },
    async handleDelete(id){ try{ await this.$confirm('确定删除?'); await request.delete(`/admin/comment/words/${id}`); this.$message.success('删除成功'); this.load() }catch{} }
  }
}
</script>
