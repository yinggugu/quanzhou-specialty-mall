<template>
  <div>
    <div class="admin-header"><h1 class="admin-title">售后管理</h1></div>
    <div class="admin-search-filter" style="display:flex;gap:10px;margin-bottom:16px;flex-wrap:wrap;align-items:center;">
      <input v-model="searchOrderNo" placeholder="订单号" class="form-control" style="width:140px;" @keydown.enter="load" />
      <input v-model="searchUsername" placeholder="用户名" class="form-control" style="width:120px;" @keydown.enter="load" />
      <select v-model="searchStatus" class="form-control" style="width:120px;" @change="load">
        <option :value="null">全部状态</option><option :value="0">待审核</option><option :value="1">待寄回</option><option :value="2">商家待收货</option><option :value="3">待退款</option><option :value="4">已完结</option><option :value="5">已驳回</option><option :value="7">寄回超时关闭</option>
      </select>
      <button class="btn btn-primary" @click="load">搜索</button>
      <button class="btn btn-secondary" @click="searchOrderNo='';searchUsername='';searchStatus=null;load()">重置</button>
    </div>
    <div class="admin-table-container"><table class="table">
      <thead><tr><th>ID</th><th>订单号</th><th>买家</th><th>退货商品ID</th><th>状态</th><th>申请时间</th><th>退款金额</th><th>操作</th></tr></thead>
      <tbody>
        <tr v-for="a in list" :key="a.id">
          <td>{{ a.id }}</td>
          <td><a href="javascript:void(0)" @click="$router.push('/admin/orders/'+a.orderId)">{{ a.orderNo }}</a></td>
          <td><a href="javascript:void(0)" @click="$router.push('/admin/users')">{{ a.username }}</a></td>
          <td>{{ a.productIds }}</td>
          <td><span :style="{color:statusColor(a.status),fontWeight:'600'}">{{ statusText(a.status) }}</span></td>
          <td>{{ a.createTime }}</td><td>¥{{ a.refundAmount||0 }}</td>
          <td><div class="table-actions">
            <a href="javascript:void(0)" class="btn btn-primary btn-sm" @click="openDetail(a)">详情</a>
            <a v-if="a.status===0" href="javascript:void(0)" class="btn btn-success btn-sm" @click="examine(a.id,true)">通过</a>
            <a v-if="a.status===0" href="javascript:void(0)" class="btn btn-danger btn-sm" @click="openReject(a.id)">驳回</a>
            <a v-if="a.status===2" href="javascript:void(0)" class="btn btn-primary btn-sm" @click="openReceive(a.id)">确认收货</a>
            <a v-if="a.status===3" href="javascript:void(0)" class="btn btn-primary btn-sm" @click="openRefund(a)">退款</a>
            <a v-if="a.status===4||a.status===5||a.status===7" href="javascript:void(0)" class="btn btn-danger btn-sm" @click="del(a.id)">删除</a>
          </div></td>
        </tr>
      </tbody>
    </table></div>
    <div v-if="total>10" class="admin-pagination">
      <a v-if="page>1" href="javascript:void(0)" class="btn btn-secondary" @click="page--;load()">上一页</a>
      <span class="admin-pagination-info">{{page}}/{{Math.ceil(total/10)}}</span>
      <a v-if="page<Math.ceil(total/10)" href="javascript:void(0)" class="btn btn-secondary" @click="page++;load()">下一页</a>
    </div>

    <!-- ✅ 驳回弹窗 -->
    <div v-if="rejectVisible" class="as-overlay"><div class="as-dialog">
      <div class="as-dialog-header"><h3 class="as-dialog-title">驳回售后</h3><span class="as-dialog-close" @click="rejectVisible=false">✕</span></div>
      <textarea v-model="rejectReason" class="as-textarea" rows="3" placeholder="请填写驳回理由(必填)"></textarea>
      <div class="as-dialog-actions"><button class="as-btn-cancel" @click="rejectVisible=false">取消</button><button class="as-btn-submit" :disabled="submitting" @click="doReject">{{submitting?'提交中...':'确认驳回'}}</button></div>
    </div></div>

    <!-- ✅ 确认收货弹窗 -->
    <div v-if="receiveVisible" class="as-overlay"><div class="as-dialog">
      <div class="as-dialog-header"><h3 class="as-dialog-title">确认收到退货</h3><span class="as-dialog-close" @click="receiveVisible=false">✕</span></div>
      <p class="as-dialog-text">确认已收到买家寄回的商品吗？</p>
      <div class="as-dialog-actions"><button class="as-btn-cancel" @click="receiveVisible=false">取消</button><button class="as-btn-submit" :disabled="submitting" @click="doReceive">{{submitting?'提交中...':'确认收货'}}</button></div>
    </div></div>

    <!-- ✅ 退款弹窗（全额/部分退款） -->
    <div v-if="refundVisible" class="as-overlay"><div class="as-dialog" style="max-height:90vh;overflow-y:auto;">
      <div class="as-dialog-header"><h3 class="as-dialog-title">确认退款</h3><span class="as-dialog-close" @click="refundClose">✕</span></div>
      <p class="as-dialog-text" style="margin-bottom:8px;">退款金额（上限 ¥{{originRefundAmount}}）：</p>
      <el-input-number v-model="refundAmount" :min="1" :max="originRefundAmount" :precision="2" style="width:100%;" />
      <!-- 部分退款专属模块 -->
      <template v-if="refundAmount < originRefundAmount">
        <p class="as-upload-title" style="margin-top:14px;">退款凭证（最多5张）<span style="color:#e74c3c;"> *必传</span></p>
        <div style="display:flex;gap:8px;flex-wrap:wrap;margin-bottom:8px;">
          <div v-for="(img,i) in refundImgArr" :key="i" class="as-thumb-wrap" @click="previewImg=img;previewVisible=true">
            <img v-if="img" :src="img" class="as-thumb-img" /><span class="as-thumb-del" @click.stop="refundImgArr.splice(i,1)">✕</span>
          </div>
          <label v-if="refundImgArr.length<5" class="as-upload-btn"><input type="file" accept="image/jpeg,image/png" style="display:none;" @change="onRefundImgUpload" /><span>+</span></label>
        </div>
        <p class="as-upload-title">退款描述（{{refundDesc.length}}/200字）<span style="color:#e74c3c;"> *必填</span></p>
        <textarea v-model="refundDesc" class="as-textarea" rows="3" :placeholder="'请描述退款原因... ('+refundDesc.length+'/200)'" maxlength="200"></textarea>
      </template>
      <div class="as-dialog-actions" style="margin-top:16px;"><button class="as-btn-cancel" @click="refundClose">取消</button><button class="as-btn-submit" :disabled="submitting" @click="doRefund">{{submitting?'提交中...':'确认退款'}}</button></div>
    </div></div>

    <!-- ✅ 售后详情弹窗（暖棕主题） -->
    <div v-if="detailVisible" class="as-overlay"><div class="as-dialog" style="max-height:85vh;overflow-y:auto;">
      <div class="as-dialog-header"><h3 class="as-dialog-title">售后详情</h3><span class="as-dialog-close" @click="detailVisible=false">✕</span></div>
      <div v-if="detail" class="as-detail-content">
        <div class="as-row"><span>售后单号</span><span>{{detail.id}}</span></div>
        <div class="as-row"><span>订单号</span><span>{{detail.orderNo}}</span></div>
        <div class="as-row"><span>买家</span><span>{{detail.username}}</span></div>
        <div class="as-row"><span>原因</span><span>{{detail.returnReason}}</span></div>
        <div class="as-row"><span>状态</span><span :style="{color:statusColor(detail.status),fontWeight:'600'}">{{statusText(detail.status)}}</span></div>
        <div class="as-row"><span>说明</span><span>{{detail.descText||'-'}}</span></div>
        <div class="as-row"><span>快递单号</span><span>{{detail.expressNo||'-'}}</span></div>
        <div class="as-row" v-if="detail.rejectContent"><span>驳回理由</span><span>{{detail.rejectContent}}</span></div>
        <div class="as-row"><span>退款金额</span><span>¥{{detail.refundAmount||0}}</span></div>
        <div v-if="detail.imgList" style="margin-top:12px;">
          <p style="color:#999288;font-size:14px;margin-bottom:6px;">凭证图片</p>
          <div style="display:flex;gap:8px;flex-wrap:wrap;">
            <img v-for="(img,i) in detail.imgList.split('||')" :key="i" :src="img" style="width:80px;height:80px;object-fit:cover;border-radius:8px;cursor:pointer;border:1px solid #ede0d2;" @click="previewImg=img;previewVisible=true" />
          </div>
        </div>
      </div>
    </div></div>
    <!-- 图片大图预览 -->
    <div v-if="previewVisible" class="as-overlay" @click="previewVisible=false"><img v-if="previewImg" :src="previewImg" style="max-width:90vw;max-height:90vh;border-radius:10px;" /></div>
  </div>
</template>
<script>
import request from '@/api/request'
export default {
  name:'AdminAfterSale',
  data(){return{list:[],page:1,total:0,searchOrderNo:'',searchUsername:'',searchStatus:null,rejectVisible:false,rejectReason:'',rejectId:null,refundVisible:false,refundAmount:0,refundId:null,originRefundAmount:0,refundImgArr:[],refundDesc:'',receiveVisible:false,receiveId:null,previewVisible:false,previewImg:'',detailVisible:false,detail:null,submitting:false}},
  mounted(){this.load()},
  methods:{
    async load(){try{const r=await request.get('/admin/afterSale/list',{params:{orderNo:this.searchOrderNo,username:this.searchUsername,status:this.searchStatus,page:this.page,pageSize:10}});if(r.code===200&&r.data){this.list=r.data.list||[];this.total=r.data.total||0}}catch(e){}},
    statusText(s){const m={0:'待审核',1:'待寄回',2:'商家待收货',3:'待退款',4:'已完结',5:'已驳回',7:'寄回超时关闭'};return m[s]||s},
    statusColor(s){const m={0:'#e67e22',1:'#3498db',2:'#7f8c8d',3:'#966c47',4:'#27ae60',5:'#e74c3c',7:'#999'};return m[s]||'#333'},
    async openDetail(a){this.detail=a;this.detailVisible=true},
    async examine(id,approve){await request.post('/admin/afterSale/examine',{id,approve});this.$message.success('审核通过');this.load()},
    openReject(id){this.rejectId=id;this.rejectReason='';this.rejectVisible=true;this.submitting=false},
    async doReject(){if(!this.rejectReason.trim())return this.$message.warning('请填写驳回理由');this.submitting=true;try{await request.post('/admin/afterSale/examine',{id:this.rejectId,approve:false,rejectContent:this.rejectReason});this.$message.success('已驳回');this.rejectVisible=false;this.load()}catch(e){this.$message.error('操作失败')}finally{this.submitting=false}},
    openReceive(id){this.receiveId=id;this.receiveVisible=true;this.submitting=false},
    async doReceive(){this.submitting=true;try{await request.post('/admin/afterSale/receiveGoods',{id:this.receiveId});this.$message.success('已确认收货');this.receiveVisible=false;this.load()}catch(e){this.$message.error('操作失败')}finally{this.submitting=false}},
    openRefund(a){this.refundId=a.id;this.originRefundAmount=a.refundEstimate||a.refundAmount||0;this.refundAmount=this.originRefundAmount;this.refundImgArr=[];this.refundDesc='';this.refundVisible=true;this.submitting=false},
    refundClose(){this.refundVisible=false;this.refundImgArr=[];this.refundDesc=''},
    async onRefundImgUpload(e){const f=e.target.files[0];if(!f)return;if(!['image/jpeg','image/png'].includes(f.type)){this.$message.warning('仅支持jpg/png');return};if(f.size>10*1024*1024){this.$message.warning('图片≤10MB');return};e.target.value='';try{const fd=new FormData();fd.append('file',f);const res=await request.post('/upload',fd,{headers:{'Content-Type':'multipart/form-data'}});if(res.code===200&&res.data.url){this.refundImgArr.push(res.data.url)}else{this.$message.error('上传失败')}}catch(err){this.$message.error('上传失败')}},
    async doRefund(){if(this.refundAmount<this.originRefundAmount){if(this.refundImgArr.length===0){this.$message.warning('部分退款必须上传凭证');return};if(!this.refundDesc.trim()){this.$message.warning('请填写退款描述');return}};this.submitting=true;try{await request.post('/admin/afterSale/refund',{id:this.refundId,amount:this.refundAmount,refundImgs:this.refundImgArr.join(','),refundDesc:this.refundDesc});this.$message.success('退款成功');this.refundClose();this.load()}catch(e){console.error(e);this.$message.error(e.response?.data?.message||e.message||'操作失败')}finally{this.submitting=false}},
    async del(id){try{await this.$confirm('确定删除?');await request.delete('/admin/afterSale/delete/'+id);this.$message.success('已删除');this.load()}catch{}}
  }
}
</script>
<style scoped>
.as-overlay{position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.3);z-index:2000;display:flex;align-items:center;justify-content:center;}
.as-dialog{width:460px;max-width:92vw;background:#faf6ee;border:1px solid #ede0d2;border-radius:16px;box-shadow:0 2px 8px rgba(92,51,23,0.04),0 8px 32px rgba(92,51,23,0.08),0 20px 64px rgba(92,51,23,0.06);padding:28px 30px 24px;box-sizing:border-box;font-family:inherit;}
.as-dialog-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;}
.as-dialog-title{margin:0;font-size:1.1rem;font-weight:600;color:#4a3322;}
.as-dialog-close{font-size:18px;color:#b8a899;cursor:pointer;line-height:1;transition:color .2s;}
.as-dialog-close:hover{color:#966c47;}
.as-dialog-text{color:#4a3322;font-size:16px;line-height:1.7;margin-bottom:16px;}
.as-textarea{width:100%;box-sizing:border-box;background:#fffefc;border:1.5px solid #d9c8b8;border-radius:10px;padding:12px 16px;font-size:15px;color:#4a3322;outline:none;resize:vertical;font-family:inherit;margin-bottom:16px;}
.as-textarea:focus{border-color:#966c47;}
.as-dialog-actions{display:flex;gap:12px;justify-content:flex-end;}
.as-btn-cancel{padding:10px 24px;background:#fff;color:#4a3322;border:2px solid #966c47;border-radius:20px;font-size:15px;cursor:pointer;font-family:inherit;}
.as-btn-cancel:hover{background:#fdfaf5;}
.as-btn-submit{padding:10px 28px;background:#966c47;color:#fff;border:none;border-radius:20px;font-size:15px;cursor:pointer;font-family:inherit;}
.as-btn-submit:hover{background:#7d5a3a;}
.as-btn-submit:disabled{opacity:.6;cursor:not-allowed;}
.as-detail-content .as-row{display:flex;justify-content:space-between;padding:6px 0;color:#4a3322;font-size:15px;border-bottom:1px solid #f8f3ec;}
.as-detail-content .as-row span:first-child{color:#999288;}
.as-upload-title{color:#999288;font-size:13px;margin-bottom:4px;}
.as-upload-btn{width:80px;height:80px;border:2.5px dashed #b8a090;border-radius:10px;display:flex;align-items:center;justify-content:center;font-size:32px;color:#b8a090;cursor:pointer;flex-shrink:0;transition:all .2s;}
.as-upload-btn:hover{border-color:#966c47;color:#966c47;}
.as-thumb-wrap{width:80px;height:80px;border-radius:10px;overflow:hidden;position:relative;cursor:pointer;flex-shrink:0;}
.as-thumb-img{width:100%;height:100%;object-fit:cover;}
.as-thumb-del{position:absolute;top:2px;right:2px;width:18px;height:18px;background:rgba(0,0,0,0.5);color:#fff;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:11px;}
</style>
