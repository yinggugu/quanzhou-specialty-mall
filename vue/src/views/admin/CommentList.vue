<template>
  <div>
    <div class="admin-header"><h1 class="admin-title">评价管理</h1></div>

    <!-- 搜索 + 筛选 -->
    <div class="admin-search-filter" style="display:flex;gap:10px;margin-bottom:16px;flex-wrap:wrap;align-items:center;">
      <input v-model="searchGoodsName" placeholder="商品名称搜索" class="form-control" style="width:200px;" @keydown.enter="loadList" />
      <select v-model="scoreFilter" class="form-control" style="width:120px;" @change="loadList">
        <option value="">全部评分</option><option value="good">好评(4-5星)</option><option value="mid">中评(3星)</option><option value="bad">差评(1-2星)</option>
      </select>
      <button class="btn btn-primary" @click="loadList">搜索</button>
      <button class="btn btn-secondary" @click="showWordsDialog = true">敏感词配置</button>
    </div>

    <!-- 表格 -->
    <div class="admin-table-container">
      <table class="table">
        <thead><tr><th>ID</th><th>商品</th><th>买家</th><th>星级</th><th>内容</th><th>图片</th><th>匿名</th><th>时间</th><th>回复</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="c in list" :key="c.id">
            <td>{{ c.id }}</td>
            <td>{{ c.goodsName }}</td>
            <td>{{ c.username }}</td>
            <td>{{ '★'.repeat(c.score) }}{{ '☆'.repeat(5-c.score) }}</td>
            <td style="max-width:180px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">{{ c.content }}</td>
            <td>{{ Array.isArray(c.imgList) ? c.imgList.length : 0 }}张</td>
            <td>{{ c.isAnonymous ? '是' : '否' }}</td>
            <td>{{ c.createTime }}</td>
            <td>{{ c.replyContent || '-' }}</td>
            <td>
              <div class="table-actions">
                <a href="javascript:void(0)" class="btn btn-primary btn-sm" @click="openReply(c)">回复</a>
                <a href="javascript:void(0)" class="btn btn-danger btn-sm" @click="handleHide(c.id)">删除</a>
              </div>
            </td>
          </tr>
          <tr v-if="list.length===0"><td colspan="10" style="text-align:center;padding:40px;">暂无评价数据</td></tr>
        </tbody>
      </table>
    </div>

    <div v-if="totalPages>1" class="admin-pagination">
      <a v-if="page>1" href="javascript:void(0)" class="btn btn-secondary" @click="page--;loadList()">上一页</a>
      <span class="admin-pagination-info">第{{ page }}/{{ totalPages }}页</span>
      <a v-if="page<totalPages" href="javascript:void(0)" class="btn btn-secondary" @click="page++;loadList()">下一页</a>
    </div>

    <!-- ✅ 回复弹窗 — 暖棕米杏主题 -->
    <div v-if="replyDialogVisible" class="reply-overlay">
      <div class="reply-dialog">
        <div class="reply-header">
          <h3 class="reply-title">回复评价</h3>
          <span class="reply-close" @click="replyDialogVisible=false">✕</span>
        </div>
        <textarea v-model="replyContent" class="reply-textarea" rows="4" placeholder="请输入回复内容"></textarea>
        <div class="reply-buttons">
          <button class="reply-btn reply-btn-cancel" @click="replyDialogVisible=false">取消</button>
          <button class="reply-btn reply-btn-submit" :disabled="submitting" @click="submitReply">{{submitting?'提交中...':'确认回复'}}</button>
        </div>
      </div>
    </div>

    <!-- 敏感词弹窗 -->
    <el-dialog title="敏感词配置" :visible.sync="showWordsDialog" width="500px">
      <div style="display:flex;gap:8px;margin-bottom:12px;">
        <el-input v-model="newWord" placeholder="输入敏感词" style="flex:1;" /><el-button type="primary" @click="addWord">添加</el-button>
      </div>
      <el-tag v-for="w in words" :key="w.id" closable @close="delWord(w.id)" style="margin:4px;">{{ w.word || w }}</el-tag>
      <span v-if="words.length===0">暂无敏感词</span>
    </el-dialog>
  </div>
</template>

<script>
import { adminCommentList, hideComment, replyComment, getSensitiveWords, addSensitiveWord, deleteSensitiveWord } from '@/api/product'
export default {
  name: 'AdminCommentList',
  data() {
    return { list: [], page: 1, totalPages: 1, searchGoodsName: '', scoreFilter: '', replyDialogVisible: false, replyContent: '', replyId: null, showWordsDialog: false, words: [], newWord: '', submitting: false }
  },
  mounted() { this.loadList() },
  methods: {
    async loadList() {
      try {
        const res = await adminCommentList({ goodsName: this.searchGoodsName, scoreFilter: this.scoreFilter, page: this.page, pageSize: 10 })
        if (res.code===200 && res.data) { this.list = res.data.list || []; this.totalPages = Math.ceil((res.data.total||0)/10) || 1 }
      } catch(e) { console.error(e) }
    },
    async handleHide(id) { try { await this.$confirm('确定删除?'); await hideComment(id); this.$message.success('已删除'); this.loadList() } catch {} },
    openReply(c) { this.replyId = c.id; this.replyContent = c.replyContent || ''; this.replyDialogVisible = true; this.submitting = false },
    async submitReply() { this.submitting = true; try { await replyComment(this.replyId, this.replyContent); this.$message.success('回复成功'); this.replyDialogVisible = false; this.loadList() } catch(e) { this.$message.error('操作失败') } finally { this.submitting = false } },
    async loadWords() { const res = await getSensitiveWords(); this.words = (res.data||[]).map((w,i)=>({id:i,word:w})) },
    async addWord() { if(!this.newWord.trim()) return; await addSensitiveWord(this.newWord.trim()); this.$message.success('添加成功'); this.newWord=''; this.loadWords() },
    async delWord(id) { await deleteSensitiveWord(id); this.$message.success('删除成功'); this.loadWords() }
  },
  watch: { showWordsDialog(v) { if(v) this.loadWords() } }
}
</script>

<style scoped>
/* ========================================
   回复弹窗 — 暖棕米杏主题（全站统一）
   ======================================== */
.reply-overlay {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0;
  background: rgba(0,0,0,0.3); z-index: 2000;
  display: flex; align-items: center; justify-content: center;
}
.reply-dialog {
  width: 460px; max-width: 92vw;
  background: #fdfaf5;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(92,51,23,0.04), 0 8px 32px rgba(92,51,23,0.08), 0 20px 64px rgba(92,51,23,0.06);
  padding: 28px 30px 24px; box-sizing: border-box; font-family: inherit;
}
.reply-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 20px;
}
.reply-title {
  margin: 0; font-size: 1.1rem; font-weight: 600; color: #4a3322;
}
.reply-close {
  font-size: 18px; color: #b8a899; cursor: pointer; line-height: 1;
  transition: color 0.2s;
}
.reply-close:hover { color: #966c47; }

.reply-textarea {
  width: 100%; box-sizing: border-box;
  background: #fffefc; border: 1.5px solid #d9c8b8; border-radius: 10px;
  padding: 12px 16px; font-size: 0.95rem; color: #4a3322;
  outline: none; resize: vertical; font-family: inherit;
  transition: border-color 0.2s;
}
.reply-textarea:focus { border-color: #966c47; }
.reply-textarea::placeholder { color: #b8a899; }

.reply-buttons {
  display: flex; gap: 12px; margin-top: 20px; justify-content: flex-end;
}
.reply-btn {
  padding: 10px 24px; border-radius: 20px; font-size: 0.95rem;
  font-weight: 500; cursor: pointer; border: none; font-family: inherit;
  transition: all 0.2s;
}
.reply-btn-cancel {
  background: #fff; color: #4a3322; border: 2px solid #966c47;
}
.reply-btn-cancel:hover { background: #fdfaf5; }
.reply-btn-submit {
  background: #966c47; color: #fff;
  box-shadow: 0 2px 8px rgba(150,108,71,0.3);
}
.reply-btn-submit:hover { background: #7d5a3a; }
.reply-btn-submit:disabled { opacity: .6; cursor: not-allowed; }
</style>
