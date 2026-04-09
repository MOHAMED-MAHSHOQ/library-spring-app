import { motion } from 'framer-motion';
import Card from './Card';

function StatCard({ title, value, hint }) {
  return (
    <motion.div initial={{ opacity: 0, y: 14 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.22 }}>
      <Card className="kpi" elevated>
        <p className="muted kpi-title">{title}</p>
        <h2 className="kpi-value">{value}</h2>
        {hint ? <p className="muted kpi-hint">{hint}</p> : null}
      </Card>
    </motion.div>
  );
}

export default StatCard;
