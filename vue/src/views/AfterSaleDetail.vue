<template>
  <div class="container" style="padding:8px 0;">
    <div class="as-detail" v-if="detail">
      <!-- 顶部进度条 -->
      <div class="as-steps">
        <div v-for="(s,i) in steps" :key="i" class="as-step" :class="{active:i<=currentStep,reject:detail.status===5}">
          <div class="as-step-dot" :style="{background:detail.status===5?(i<steps.length-1?'#e74c3c':'#e0d5c5'):(i<=currentStep?stepColor(i):'#e0d5c5')}"></div>
          <span :style="{color:detail.status===5?(i<steps.length-1?'#e74c3c':'#b8a899'):(i<=currentStep?stepColor(i):'#b8a899')}">{{s}}</span>
        </div>
      </div>

      <!-- 统一大卡片 -->
      <div class="as-card">
        <!-- 分区1：状态标题 + 倒计时 -->
        <div class="as-section">
          <div class="as-status-line">
            <span class="as-status-icon">🕐</span>
            <span class="as-status-tag">{{statusText(detail.status)}}</span>
          </div>
          <div v-if="detail.status===0&&detail.auditTimeLeft" class="as-countdown">商家审核倒计时 <span class="as-countdown-num">{{detail.auditTimeLeft}}</span></div>
          <div v-if="detail.status===1&&detail.returnTimeLeft" class="as-countdown">商家等待您寄回倒计时 <span class="as-countdown-num">{{detail.returnTimeLeft}}</span></div>
          <div v-if="detail.status===3&&detail.verifyTimeLeft" class="as-countdown">商家退款核验倒计时 <span class="as-countdown-num">{{detail.verifyTimeLeft}}</span></div>
          <div v-if="detail.status===5&&detail.rejectContent" class="as-reject-box">驳回理由：{{detail.rejectContent}}</div>
        </div>
        <div class="as-divider"></div>

        <!-- 板块标题：售后详情 -->
        <h3 class="as-section-title">售后详情</h3>

        <!-- 分区2：退货商品 -->
        <div class="as-section">
          <div v-if="detail.items&&detail.items.length>0">
            <div v-for="item in detail.items" :key="item.id" class="as-product-row">
              <img v-if="getImg(item.productImage)" :src="getImg(item.productImage)" class="as-product-img" />
              <div v-else class="as-product-img as-product-img-placeholder">无图</div>
              <div class="as-product-info">
                <p class="as-product-name">{{item.productName}}</p>
                <p class="as-product-price">单价 ¥{{item.productPrice}} × {{item.quantity}} &nbsp; 小计 <span class="as-amount">¥{{item.totalPrice}}</span></p>
              </div>
            </div>
          </div>
        </div>

        <!-- 分区3：申请信息（退货原因保留，仅去掉上方分割线） -->
        <div class="as-section">
          <div class="as-row"><span>退货原因</span><span>{{detail.returnReason}}</span></div>
          <div class="as-row"><span>补充说明</span><span>{{detail.descText||'-'}}</span></div>
          <div v-if="detail.imgList" style="display:flex;gap:8px;margin-top:8px;flex-wrap:wrap;">
            <img v-for="(img,i) in detail.imgList.split('||')" :key="i" :src="img" class="as-thumb" @click="previewImg=img;imgVisible=true" />
          </div>
          <div v-else class="as-no-img">无凭证图片</div>
        </div>

        <!-- 物流（仅status=1） -->
        <template v-if="detail.status===1">
          <div class="as-divider"></div>
          <h3 class="as-section-title">商家退货地址</h3>
          <div class="as-section">
            <p class="as-logistics-text">收件人：{{ returnContact }}</p>
            <p class="as-logistics-text">地址：{{ returnAddress }}</p>
            <p class="as-freight-tip">退货产生的运费需由您自行承担</p>
            <div style="display:flex;gap:8px;margin-top:10px;">
              <input v-model="expressInput" class="as-input" placeholder="快递单号(仅数字字母)" style="flex:1;" @input="expressInput=expressInput.replace(/[^a-zA-Z0-9]/g,'')" />
              <button class="btn btn-primary" @click="submitExpress">提交</button>
            </div>
          </div>
        </template>
        <template v-if="detail.status>=2&&detail.expressNo">
          <div class="as-divider"></div>
          <div class="as-section"><div class="as-row"><span>快递单号</span><span>{{detail.expressNo}}</span></div></div>
        </template>
        <!-- 收货核验状态（status≥2展示） -->
        <template v-if="detail.status>=2">
          <div class="as-section"><div class="as-row"><span>收货核验状态</span><span>{{confirmStatusText}}</span></div></div>
        </template>

        <!-- 退款（status≥3） -->
        <template v-if="detail.status>=3">
          <div class="as-divider"></div>
          <div class="as-section">
            <div class="as-row"><span>预估退款</span><span class="as-amount">¥{{detail.refundEstimate||0}}</span></div>
            <div class="as-row"><span>实际退款</span><span class="as-amount">¥{{detail.refundActual||detail.refundEstimate||0}}</span></div>
            <p v-if="detail.status===4" class="as-refund-done">款项已原路退回支付账户</p>
          </div>
        </template>

        <!-- 操作按钮区 -->
        <template v-if="detail.status===0||detail.status===1">
          <div class="as-section as-btn-right"><button class="as-cancel-btn" @click="cancelVisible=true">撤销售后</button></div>
        </template>
        <template v-if="detail.status===2">
          <div class="as-section as-btn-right"><button class="as-cancel-btn" @click="viewExpress">查看物流</button></div>
        </template>
        <template v-if="detail.status===3">
          <div class="as-section as-btn-right"><button class="as-cancel-btn" @click="viewRefund">查看退款</button></div>
        </template>
        <template v-if="detail.status===4">
          <div class="as-section as-btn-right"><button class="as-cancel-btn" @click="viewDetail">查看详情</button></div>
        </template>

        <div class="as-divider"></div>
        <!-- 操作记录 -->
        <div class="as-section" v-if="detail.logs&&detail.logs.length>0">
          <div class="as-timeline">
            <div v-for="(l,i) in detail.logs" :key="i" class="as-timeline-item">
              <div class="as-timeline-dot" :style="{background:logColor(l.type)}"></div>
              <div class="as-timeline-body">
                <span class="as-log-type">{{l.type}}</span>
                <span class="as-log-time">{{l.time}}</span>
                <div v-if="l.content" class="as-log-content">{{l.content}}</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 加载中 / 无数据 -->
    <div v-else class="as-empty">
      <p v-if="loading">加载中...</p>
      <p v-else>暂无售后数据</p>
    </div>

    <!-- 撤销弹窗 -->
    <div v-if="cancelVisible" class="as-overlay">
      <div class="as-dialog">
        <div class="as-dialog-header"><h3 class="as-dialog-title">撤销售后</h3><span class="as-dialog-close" @click="cancelVisible=false">✕</span></div>
        <p class="as-dialog-text">确定要撤销本次退货申请吗？撤销后本次申请作废，重新发起需再次填写全部退货信息、上传凭证</p>
        <div class="as-dialog-actions"><button class="as-dialog-btn" @click="doCancel">确认撤销</button></div>
      </div>
    </div>
    <el-dialog :visible.sync="imgVisible" width="500px"><img v-if="previewImg" :src="previewImg" style="width:100%;border-radius:8px;" /></el-dialog>
  </div>
</template>

<script>
import request from '@/api/request'
export default {
  name:'AfterSaleDetail',
  data(){return{detail:null,loading:true,steps:['提交申请','商家审核','用户寄回','商家收货','退款处理','售后完成'],expressInput:'',cancelVisible:false,imgVisible:false,previewImg:'',returnContact:process.env.VUE_APP_RETURN_CONTACT||'商城售后部',returnAddress:process.env.VUE_APP_RETURN_ADDRESS||'请联系在线客服获取退货地址'}},
  computed:{currentStep(){const m={0:0,1:1,2:2,3:3,4:4,5:-1};return this.detail?m[this.detail.status]:0},
    confirmStatusText(){if(!this.detail)return'';if(this.detail.confirmTime)return this.detail.confirmStatus+' '+this.detail.confirmTime;return this.detail.confirmStatus||'';}},
  mounted(){this.load()},
  methods:{
    async load(){this.loading=true;const q=this.$route.query;try{const r=await request.get('/afterSale/userDetail',{params:{id:q.id,orderId:q.orderId}});if(r.code===200){this.detail=r.data}}catch(e){}this.loading=false;window.scrollTo(0,0)},
    statusText(s){const m={0:'待商家审核',1:'待用户寄回',2:'商家待收货',3:'待退款',4:'售后完结',5:'已驳回',6:'审核超时关闭',7:'寄回超时关闭'};return m[s]||s},
    statusColor(s){const m={0:'#966c47',1:'#966c47',2:'#966c47',3:'#966c47',4:'#966c47',5:'#e74c3c',6:'#999',7:'#999'};return m[s]||'#333'},
    stepColor(i){const m=['#966c47','#966c47','#966c47','#966c47','#966c47','#966c47'];return m[i]||'#e0d5c5'},
    logColor(t){const m={申请:'#966c47',撤销:'#e74c3c','系统自动审核':'#3498db','审核通过':'#27ae60',驳回:'#e74c3c','填写物流':'#3498db','确认收货':'#7f8c8d',退款:'#966c47'};return m[t]||'#b8a899'},
    async submitExpress(){if(!this.expressInput||!/^[a-zA-Z0-9]+$/.test(this.expressInput))return this.$message.warning('快递单号仅允许数字和字母');await request.post('/afterSale/saveExpress',{id:this.detail.id,expressNo:this.expressInput});this.$message.success('已提交');this.load()},
    async doCancel(){try{await request.post('/afterSale/cancel',{id:this.detail.id});this.$message.success('已成功撤销售后申请');this.cancelVisible=false;this.$router.push('/orders')}catch(e){this.$message.error('撤销失败，请重试')}},
    getImg(p){return this.$resolveImageUrl(p)},
    viewExpress(){this.$message.info('快递单号：'+this.detail.expressNo)},
    viewRefund(){this.$message.info('退款金额：¥'+(this.detail.refundActual||this.detail.refundEstimate||0))},
    viewDetail(){window.scrollTo(0,0)}
  }
}
</script>

<style scoped>
.as-detail{max-width:100%;margin:0 auto;display:flex;flex-direction:column;gap:12px;padding:0 8px;}
.as-empty{text-align:center;padding:60px 20px;color:#999288;font-size:17px;}
.as-steps{display:flex;background:#fff;border-radius:14px;padding:32px 20px 24px;box-shadow:0 2px 12px rgba(92,51,23,0.05);overflow-x:auto;}
.as-step{flex:1;text-align:center;border-top:3px solid #e8ddd0;padding-top:12px;font-size:15px;min-width:56px;font-weight:500;transition:border-color .3s;}
.as-step.active{border-color:#966c47;}
.as-step-dot{width:14px;height:14px;border-radius:50%;margin:-18px auto 6px;transition:background .3s;}
.as-card{background:#fff;border-radius:14px;padding:30px 44px;box-shadow:0 2px 12px rgba(92,51,23,0.05);}
.as-section{padding:3px 0;text-align:left;}
.as-divider{height:1px;background:#ede0d2;margin:16px 0;}

/* 板块标题 */
.as-section-title{color:#966c47;font-size:22px;font-weight:700;margin:8px 0 12px;}

/* 统一文字 */
.as-row{display:flex;justify-content:flex-start;align-items:flex-start;padding:7px 0;color:#4a3322;font-size:17px;line-height:1.5;gap:12px;}
.as-row span:first-child{color:#999288;font-size:17px;flex-shrink:0;min-width:80px;}
.as-row span:last-child{color:#4a3322;flex:1;}

/* 状态行：图标 + 标题并排 */
.as-status-line{display:flex;align-items:center;gap:8px;}
.as-status-icon{font-size:30px;line-height:1;}
.as-status-tag{color:#966c47;font-size:28px;font-weight:700;}

/* 倒计时 — 灰色小字，状态下方 */
.as-countdown{color:#999288;font-size:14px;margin-top:6px;}
.as-countdown-num{color:#966c47;font-weight:400;}

/* 驳回 */
.as-reject-box{background:linear-gradient(135deg,#fff5f5,#fef0f0);color:#e74c3c;padding:16px 22px;border-radius:10px;margin-top:10px;font-size:17px;line-height:1.5;}

/* 商品行 */
.as-product-row{display:flex;gap:16px;padding:8px 0;align-items:center;}
.as-product-img{width:88px;height:88px;border-radius:10px;object-fit:cover;flex-shrink:0;border:1px solid #f0e8dc;}
.as-product-img-placeholder{background:#faf6f0;display:flex;align-items:center;justify-content:center;color:#c4b49e;font-size:14px;}
.as-product-info{flex:1;min-width:0;}
.as-product-name{color:#4a3322;font-size:18px;font-weight:600;margin:0 0 4px;line-height:1.4;}
.as-product-price{color:#999288;font-size:17px;margin:0;}
.as-amount{color:#966c47;font-weight:600;}

/* 缩略图 */
.as-thumb{width:80px;height:80px;object-fit:cover;border-radius:10px;cursor:pointer;border:1px solid #f0e8dc;transition:transform .15s;}
.as-thumb:hover{transform:scale(1.05);}
.as-no-img{color:#988878;font-size:17px;margin-top:4px;}

/* 物流 */
.as-logistics-text{color:#4a3322;font-size:17px;}
.as-freight-tip{color:#999288;font-size:15px;margin-top:4px;}

/* 退款 */
.as-refund-done{color:#27ae60;font-size:17px;margin-top:4px;}

/* 输入框 */
.as-input{flex:1;padding:12px 20px;border:1.5px solid #e0d3c4;border-radius:22px;font-size:15px;outline:none;color:#4a3322;font-family:inherit;background:#fefdfb;transition:border-color .2s;}
.as-input:focus{border-color:#966c47;background:#fff;}

/* 操作按钮区 */
.as-btn-right{display:flex;justify-content:flex-end;margin-top:24px;}
.as-cancel-btn{padding:12px 36px;background:#faf6ee;color:#966c47;border:1.5px solid #966c47;border-radius:22px;font-size:17px;cursor:pointer;font-family:inherit;transition:all .2s;}
.as-cancel-btn:hover{background:#966c47;color:#fff;}

/* 时间线 */
.as-timeline{position:relative;padding-left:18px;}
.as-timeline::before{content:'';position:absolute;left:7px;top:4px;bottom:4px;width:1.5px;background:linear-gradient(to bottom,transparent,#e0d3c4 10%,#e0d3c4 90%,transparent);}
.as-timeline-item{display:flex;gap:12px;padding:7px 0;position:relative;}
.as-timeline-dot{width:10px;height:10px;border-radius:50%;flex-shrink:0;margin-top:5px;position:relative;z-index:1;box-shadow:0 0 0 2px #fff;}
.as-timeline-body{flex:1;min-width:0;}
.as-log-type{font-weight:500;color:#4a3322;font-size:17px;}
.as-log-time{color:#988878;margin-left:8px;font-size:15px;}
.as-log-content{color:#988878;font-size:15px;margin-top:2px;}

/* ======== 撤销弹窗 ======== */
.as-overlay{position:fixed;top:0;left:0;right:0;bottom:0;background:rgba(0,0,0,0.3);z-index:2000;display:flex;align-items:center;justify-content:center;}
.as-dialog{width:440px;max-width:92vw;background:#faf6ee;border:1px solid #ede0d2;border-radius:16px;box-shadow:0 2px 8px rgba(92,51,23,0.04),0 8px 32px rgba(92,51,23,0.08),0 20px 64px rgba(92,51,23,0.06);padding:28px 30px 24px;box-sizing:border-box;font-family:inherit;}
.as-dialog-header{display:flex;justify-content:space-between;align-items:center;margin-bottom:16px;}
.as-dialog-title{margin:0;font-size:1.1rem;font-weight:600;color:#4a3322;}
.as-dialog-close{font-size:18px;color:#b8a899;cursor:pointer;line-height:1;transition:color .2s;}
.as-dialog-close:hover{color:#966c47;}
.as-dialog-text{color:#4a3322;font-size:16px;line-height:1.7;margin-bottom:20px;}
.as-dialog-actions{display:flex;justify-content:center;}
.as-dialog-btn{padding:12px 48px;background:#966c47;color:#fff;border:none;border-radius:22px;font-size:17px;cursor:pointer;font-family:inherit;transition:background .2s;}
.as-dialog-btn:hover{background:#7d5a3a;}
</style>
